package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.common.enums.PriorityEnum;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.CurrentUser;
import com.cqust.repair.common.util.XssUtils;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.infra.track.OrderTrack;
import com.cqust.repair.module.basedata.entity.FaultType;
import com.cqust.repair.module.basedata.entity.Room;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.FaultTypeService;
import com.cqust.repair.module.basedata.service.RoomService;
import com.cqust.repair.module.file.service.FileService;
import com.cqust.repair.module.order.dto.CreateOrderRequest;
import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.ManagerOrderQuery;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderImageVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.entity.OrderImage;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.mapper.OrderImageMapper;
import com.cqust.repair.module.order.mapper.WorkOrderMapper;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单原子服务：创建、查询、详情、列表。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_ORDERS_PER_MINUTE = 5;
    private static final long IDEMPOTENT_SECONDS = 30;
    private static final int DUPLICATE_RETRY = 3;

    private final WorkOrderMapper workOrderMapper;
    private final OrderImageMapper orderImageMapper;
    private final BuildingService buildingService;
    private final RoomService roomService;
    private final FaultTypeService faultTypeService;
    private final FileService fileService;
    private final UserService userService;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderTrackService orderTrackService;
    private final RepairService repairService;
    private final EvaluationService evaluationService;
    private final OrderRoundService orderRoundService;
    private final TimeoutQueueService timeoutQueueService;
    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;

    /**
     * 学生提交报修。
     *
     * <p>创建时报修人为当前学生，priority / expect_hours / deadline 全部由服务端计算。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderVO create(Long studentId, CreateOrderRequest request) {
        SysUser student = userService.getRequired(studentId);
        if (!RoleEnum.STUDENT.name().equals(student.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅学生可提交报修");
        }
        checkIdempotent(studentId, request);
        checkRateLimit(studentId);

        buildingService.getById(request.getBuildingId());
        Room room = roomService.getByIdInBuilding(request.getRoomId(), request.getBuildingId());
        FaultType faultType = faultTypeService.getEnabledById(request.getFaultTypeId());
        validateImages(request.getImageFileIds());

        int priority = normalizePriority(faultType.getDefaultPriority());
        int expectHours = calculateExpectHours(priority, faultType.getExpectHours());
        LocalDateTime now = LocalDateTime.now();

        WorkOrder order = new WorkOrder();
        order.setStudentId(studentId);
        order.setBuildingId(request.getBuildingId());
        order.setRoomId(request.getRoomId());
        order.setRoomNo(room.getRoomNo());
        order.setFaultTypeId(faultType.getId());
        order.setFaultTypeName(faultType.getName());
        order.setFaultCategory(faultType.getCategory());
        order.setDescription(XssUtils.escape(request.getDescription().trim()));
        order.setContactPhone(StringUtils.hasText(request.getContactPhone())
                ? request.getContactPhone() : student.getPhone());
        order.setPriority(priority);
        order.setStatus(OrderStatusEnum.CREATED.name());
        order.setExpectHours(expectHours);
        order.setDeadlineTime(now.plusHours(expectHours));
        order.setTimeoutFlag(0);
        order.setIsRework(0);
        order.setEvaluateFlag(0);
        insertWithOrderNoRetry(order);
        order.setRootOrderId(order.getId());
        order.setCurrentRound(1);
        workOrderMapper.updateById(order);
        orderRoundService.createFirstRound(order);

        insertImages(order.getId(), request.getImageFileIds());
        try {
            fileService.markSubmitted(request.getImageFileIds());
        } catch (Exception e) {
            log.warn("图片提交标记失败: orderId={}, error={}", order.getId(), e.getMessage());
        }
        orderTrackService.record(order.getId(), order.getOrderNo(), null, order.getStatus(),
                "CREATE", "学生提交报修", 1);
        timeoutQueueService.schedule(order.getId(), order.getDeadlineTime());

        CreateOrderVO vo = new CreateOrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setDeadlineTime(order.getDeadlineTime());
        return vo;
    }

    /**
     * 返修中改派：只更新受理人与派单时间，不改变工单状态。
     */
    public void reassign(Long orderId, Long assigneeId, LocalDateTime assignTime) {
        workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<WorkOrder>()
                .eq("id", orderId)
                .set("assignee_id", assigneeId)
                .set("assign_time", assignTime)
                .set("dispatch_type", "ASSIGN"));
    }

    /**
     * 查询工单，不存在抛 404。
     */
    public WorkOrder getRequired(Long orderId) {        WorkOrder order = orderId == null ? null : workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 按 id 查询工单，不存在返回 null（定时任务容错场景使用）。
     */
    public WorkOrder findById(Long orderId) {
        return orderId == null ? null : workOrderMapper.selectById(orderId);
    }

    /**
     * 完工时间早于指定时间的 FINISHED 工单（自动确认用）。
     */
    public List<WorkOrder> listFinishedBefore(LocalDateTime time, int limit) {
        return workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
                .eq("status", OrderStatusEnum.FINISHED.name())
                .le("finish_time", time)
                .orderByAsc("finish_time")
                .last("LIMIT " + clampLimit(limit)));
    }

    /**
     * 确认时间早于指定时间的 CONFIRMED 工单（自动关闭用）。
     */
    public List<WorkOrder> listConfirmedBefore(LocalDateTime time, int limit) {
        return workOrderMapper.selectList(new QueryWrapper<WorkOrder>()
                .eq("status", OrderStatusEnum.CONFIRMED.name())
                .le("confirm_time", time)
                .orderByAsc("confirm_time")
                .last("LIMIT " + clampLimit(limit)));
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }

    /**
     * 学生工单列表。
     */
    public PageResult<OrderListItemVO> pageStudentOrders(Long studentId, String status,
                                                         String keyword, long pageNum, long pageSize) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>().eq("student_id", studentId);
        if (StringUtils.hasText(status)) {
            if (OrderStatusEnum.of(status) == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "非法工单状态");
            }
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("order_no", kw)
                    .or().like("fault_type_name", kw)
                    .or().like("description", kw));
        }
        wrapper.orderByDesc("create_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, convertListItems(page.getRecords()));
    }

    /**
     * 管理员工单列表（按楼栋范围 + 多条件筛选）。
     *
     * @param managedBuildingIds 当前管理员负责的楼栋 id（为空直接返回空页）
     */
    public PageResult<OrderListItemVO> pageManagerOrders(ManagerOrderQuery query,
                                                         List<Long> managedBuildingIds) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        if (managedBuildingIds == null || managedBuildingIds.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        QueryWrapper<WorkOrder> wrapper = baseManagerWrapper(query, managedBuildingIds);
        applyFilters(wrapper, query);
        wrapper.orderByDesc("create_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, convertListItems(page.getRecords()));
    }

    /**
     * 管理员超时工单列表（timeout_flag=1，按截止时间升序）。
     */
    public PageResult<OrderListItemVO> pageTimeoutOrders(ManagerOrderQuery query,
                                                         List<Long> managedBuildingIds) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        if (managedBuildingIds == null || managedBuildingIds.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        QueryWrapper<WorkOrder> wrapper = baseManagerWrapper(query, managedBuildingIds);
        applyFilters(wrapper, query);
        wrapper.eq("timeout_flag", 1).orderByAsc("deadline_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, convertListItems(page.getRecords()));
    }

    /**
     * 维修工「我的工单」列表。
     */
    public PageResult<OrderListItemVO> pageRepairerOrders(Long repairerId, String status,
                                                          long pageNum, long pageSize) {
        long safePageNum = normalizePageNum(pageNum);
        long safePageSize = normalizePageSize(pageSize);
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>().eq("assignee_id", repairerId);
        if (StringUtils.hasText(status)) {
            if (OrderStatusEnum.of(status) == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "非法工单状态");
            }
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        return PageResult.of(page, convertListItems(page.getRecords()));
    }

    /**
     * 抢单池：本楼栋 CREATED 工单，按优先级 + 创建时间排序，排除拒单达阈值的工单。
     */
    public List<OrderListItemVO> listPool(Long buildingId, List<Long> excludedOrderIds) {
        if (buildingId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>()
                .eq("status", OrderStatusEnum.CREATED.name())
                .eq("building_id", buildingId);
        if (excludedOrderIds != null && !excludedOrderIds.isEmpty()) {
            wrapper.notIn("id", excludedOrderIds);
        }
        wrapper.orderByAsc("priority").orderByAsc("create_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(1, 50, false), wrapper);
        return convertListItems(page.getRecords());
    }

    /**
     * 看板某列前 N 条工单。
     */
    public List<OrderListItemVO> listTopByStatus(List<Long> buildingIds, String status, int limit) {
        if (buildingIds == null || buildingIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>()
                .in("building_id", buildingIds)
                .eq("status", status)
                .orderByAsc("priority")
                .orderByDesc("create_time");
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(1, limit, false), wrapper);
        return convertListItems(page.getRecords());
    }

    /**
     * 按状态分组计数（看板列头）。
     */
    public Map<String, Long> countGroupByStatus(List<Long> buildingIds) {
        Map<String, Long> result = new HashMap<>();
        if (buildingIds == null || buildingIds.isEmpty()) {
            return result;
        }
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>()
                .select("status", "COUNT(1) AS cnt")
                .in("building_id", buildingIds)
                .groupBy("status");
        List<Map<String, Object>> rows = workOrderMapper.selectMaps(wrapper);
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status != null && cnt instanceof Number number) {
                result.put(status.toString(), number.longValue());
            }
        }
        return result;
    }

    private QueryWrapper<WorkOrder> baseManagerWrapper(ManagerOrderQuery query, List<Long> managedBuildingIds) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        if (query.getBuildingId() != null) {
            if (!managedBuildingIds.contains(query.getBuildingId())) {
                throw new BizException(ResultCode.NO_PERMISSION_OF_BUILDING);
            }
            wrapper.eq("building_id", query.getBuildingId());
        } else {
            wrapper.in("building_id", managedBuildingIds);
        }
        return wrapper;
    }

    private void applyFilters(QueryWrapper<WorkOrder> wrapper, ManagerOrderQuery query) {
        if (StringUtils.hasText(query.getStatus())) {
            if (OrderStatusEnum.of(query.getStatus()) == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "非法工单状态");
            }
            wrapper.eq("status", query.getStatus());
        }
        if (query.getPriority() != null) {
            if (PriorityEnum.of(query.getPriority()) == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "优先级取值 1~4");
            }
            wrapper.eq("priority", query.getPriority());
        }
        if (query.getTimeoutFlag() != null) {
            wrapper.eq("timeout_flag", query.getTimeoutFlag());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq("assignee_id", query.getAssigneeId());
        }
        LocalDateTime begin = parseTime(query.getBeginTime(), false);
        LocalDateTime end = parseTime(query.getEndTime(), true);
        if (begin != null) {
            wrapper.ge("create_time", begin);
        }
        if (end != null) {
            wrapper.lt("create_time", end);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like("order_no", kw)
                    .or().like("room_no", kw)
                    .or().like("fault_type_name", kw)
                    .or().like("description", kw));
        }
    }

    /**
     * 时间参数解析：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss；日期型结束时间取次日零点（左闭右开）。
     */
    private LocalDateTime parseTime(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text);
                return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "时间格式不正确");
        }
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    /**
     * 工单详情（含图片、MongoDB 轨迹）。
     */
    public OrderDetailVO detail(Long orderId, CurrentUser user) {
        WorkOrder order = getRequired(orderId);
        assertCanView(order, user);
        return buildDetail(order);
    }

    /**
     * 校验当前用户是否有查看权限。
     */
    public void assertCanView(WorkOrder order, CurrentUser user) {
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        switch (user.role() == null ? "" : user.role()) {
            case "STUDENT" -> {
                if (!order.getStudentId().equals(user.userId())) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
            }
            case "REPAIRER" -> {
                if (order.getAssigneeId() == null || !order.getAssigneeId().equals(user.userId())) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
            }
            case "MANAGER" -> {
                if (!buildingService.canManage(user.userId(), order.getBuildingId())) {
                    throw new BizException(ResultCode.NO_PERMISSION_OF_BUILDING);
                }
            }
            default -> throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 列表项批量转换（楼栋名 / 维修工名一次查出，避免 N+1）。
     */
    public List<OrderListItemVO> convertListItems(List<WorkOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        var buildingNames = buildingService.nameMap();
        List<Long> assigneeIds = orders.stream()
                .map(WorkOrder::getAssigneeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        var assigneeMap = userService.mapByIds(assigneeIds);

        List<OrderListItemVO> result = new ArrayList<>(orders.size());
        for (WorkOrder order : orders) {
            OrderListItemVO vo = new OrderListItemVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setBuildingId(order.getBuildingId());
            vo.setBuildingName(buildingNames.get(order.getBuildingId()));
            vo.setRoomNo(order.getRoomNo());
            vo.setFaultTypeName(order.getFaultTypeName());
            vo.setPriority(order.getPriority());
            vo.setStatus(order.getStatus());
            OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getStatus());
            vo.setStatusLabel(statusEnum == null ? order.getStatus() : statusEnum.getLabel());
            vo.setAssigneeId(order.getAssigneeId());
            SysUser assignee = order.getAssigneeId() == null ? null : assigneeMap.get(order.getAssigneeId());
            vo.setAssigneeName(assignee == null ? null : assignee.getRealName());
            vo.setCreateTime(order.getCreateTime());
            vo.setDeadlineTime(order.getDeadlineTime());
            vo.setTimeoutFlag(order.getTimeoutFlag());
            vo.setIsRework(order.getIsRework());
            vo.setCurrentRound(order.getCurrentRound() == null ? 1 : order.getCurrentRound());
            result.add(vo);
        }
        return result;
    }

    private OrderDetailVO buildDetail(WorkOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.of(order.getStatus());
        vo.setStatusLabel(statusEnum == null ? order.getStatus() : statusEnum.getLabel());
        vo.setStudentId(order.getStudentId());
        SysUser student = userService.findById(order.getStudentId());
        vo.setStudentName(student == null ? null : student.getRealName());
        vo.setContactPhone(order.getContactPhone());
        vo.setBuildingId(order.getBuildingId());
        vo.setBuildingName(buildingService.findName(order.getBuildingId()));
        vo.setRoomId(order.getRoomId());
        vo.setRoomNo(order.getRoomNo());
        vo.setFaultTypeId(order.getFaultTypeId());
        vo.setFaultTypeName(order.getFaultTypeName());
        vo.setFaultCategory(order.getFaultCategory());
        vo.setDescription(order.getDescription());
        vo.setPriority(order.getPriority());
        vo.setExpectHours(order.getExpectHours());
        vo.setDeadlineTime(order.getDeadlineTime());
        vo.setAssigneeId(order.getAssigneeId());
        SysUser assignee = order.getAssigneeId() == null ? null : userService.findById(order.getAssigneeId());
        vo.setAssigneeName(assignee == null ? null : assignee.getRealName());
        vo.setDispatchType(order.getDispatchType());
        vo.setTimeoutFlag(order.getTimeoutFlag());
        vo.setTimeoutType(order.getTimeoutType());
        vo.setIsRework(order.getIsRework());
        vo.setSourceOrderId(order.getSourceOrderId());
        vo.setRootOrderId(order.getRootOrderId());
        vo.setCurrentRound(order.getCurrentRound() == null ? 1 : order.getCurrentRound());
        vo.setEvaluateFlag(order.getEvaluateFlag());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        vo.setAssignTime(order.getAssignTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setArriveTime(order.getArriveTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setCloseTime(order.getCloseTime());
        vo.setCancelTime(order.getCancelTime());

        List<OrderImage> images = orderImageMapper.selectList(new QueryWrapper<OrderImage>()
                .eq("order_id", order.getId())
                .orderByAsc("sort"));
        List<OrderImageVO> before = new ArrayList<>();
        List<OrderImageVO> after = new ArrayList<>();
        for (OrderImage image : images) {
            OrderImageVO imageVO = new OrderImageVO();
            imageVO.setFileId(image.getFileId());
            imageVO.setUrl("/api/v1/files/" + image.getFileId());
            imageVO.setFileName(image.getFileName());
            imageVO.setSort(image.getSort());
            imageVO.setRoundNo(image.getRoundNo());
            if ("AFTER".equals(image.getImgType())) {
                after.add(imageVO);
            } else {
                before.add(imageVO);
            }
        }
        vo.setBeforeImages(before);
        vo.setAfterImages(after);
        int currentRound = order.getCurrentRound() == null ? 1 : order.getCurrentRound();
        vo.setRepairRecord(repairService.getVO(order.getId(), currentRound));
        vo.setEvaluation(evaluationService.getVO(order.getId()));
        vo.setRounds(orderRoundService.listRounds(order.getId()));
        vo.setAcceptances(orderRoundService.listAcceptances(order.getId()));
        List<OrderTrack> tracks = orderTrackService.list(order.getId());
        vo.setTracks(tracks);
        return vo;
    }

    /**
     * 统计工单图片数量。
     *
     * @param imgType BEFORE/AFTER；为空则统计全部
     */
    public int countImages(Long orderId, String imgType) {
        QueryWrapper<OrderImage> wrapper = new QueryWrapper<OrderImage>().eq("order_id", orderId);
        if (StringUtils.hasText(imgType)) {
            wrapper.eq("img_type", imgType);
        }
        Long count = orderImageMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 追加工单图片并标记文件已提交（默认归入当前轮次）。
     */
    public void appendImages(Long orderId, String imgType, List<String> fileIds) {
        WorkOrder order = getRequired(orderId);
        int roundNo = order.getCurrentRound() == null || order.getCurrentRound() < 1
                ? 1 : order.getCurrentRound();
        appendImages(orderId, imgType, fileIds, roundNo);
    }

    /**
     * 追加工单图片（指定轮次）并标记文件已提交。
     */
    public void appendImages(Long orderId, String imgType, List<String> fileIds, Integer roundNo) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            if (!fileService.exists(fileId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "图片不存在或已失效");
            }
        }
        int sort = countImages(orderId, imgType);
        int round = roundNo == null || roundNo < 1 ? 1 : roundNo;
        for (String fileId : fileIds) {
            OrderImage image = new OrderImage();
            image.setOrderId(orderId);
            image.setRoundNo(round);
            image.setImgType(imgType);
            image.setFileId(fileId);
            image.setSort(sort++);
            orderImageMapper.insert(image);
        }
        try {
            fileService.markSubmitted(fileIds);
        } catch (Exception e) {
            log.warn("图片提交标记失败: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    /**
     * 30 秒内同一房间 + 同一故障类型视为重复提交。
     */
    private void checkIdempotent(Long studentId, CreateOrderRequest request) {
        String raw = request.getRoomId() + ":" + request.getFaultTypeId();
        String hash = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        boolean first = redisCacheService.setIfAbsent(RedisKeys.ORDER_IDEM + hash, String.valueOf(studentId),
                IDEMPOTENT_SECONDS);
        if (!first) {
            throw new BizException(ResultCode.DUPLICATE_SUBMIT);
        }
    }

    /**
     * 每人每分钟最多 5 次提交。
     */
    private void checkRateLimit(Long studentId) {
        long count = redisCacheService.increment(RedisKeys.LIMIT_SUBMIT + studentId, 60);
        if (count > MAX_ORDERS_PER_MINUTE) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "提交过于频繁，请 1 分钟后再试");
        }
    }

    private void validateImages(List<String> fileIds) {
        for (String fileId : fileIds) {
            if (!fileService.exists(fileId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "图片不存在或已失效");
            }
        }
    }

    /**
     * 工单号唯一冲突时重试 3 次（Redis 不可用时的兜底）。
     */
    private void insertWithOrderNoRetry(WorkOrder order) {
        for (int attempt = 0; attempt < DUPLICATE_RETRY; attempt++) {
            order.setOrderNo(orderNoGenerator.nextOrderNo());
            try {
                workOrderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("工单号冲突，第 {} 次重试", attempt + 1);
            }
        }
        throw new BizException(ResultCode.SYSTEM_ERROR, "工单号生成冲突，请重试");
    }

    private void insertImages(Long orderId, List<String> fileIds) {
        int sort = 0;
        for (String fileId : fileIds) {
            OrderImage image = new OrderImage();
            image.setOrderId(orderId);
            image.setRoundNo(1);
            image.setImgType("BEFORE");
            image.setFileId(fileId);
            image.setSort(sort++);
            orderImageMapper.insert(image);
        }
    }

    private int normalizePriority(Integer defaultPriority) {
        if (defaultPriority == null) {
            return PriorityEnum.P3.getValue();
        }
        return Math.max(PriorityEnum.P1.getValue(), Math.min(PriorityEnum.P4.getValue(), defaultPriority));
    }

    /**
     * 期望完成时长 = min(优先级默认时长, 故障类型期望时长)。
     */
    public int calculateExpectHours(int priority, Integer faultTypeExpectHours) {
        Integer configHours = appProperties.getOrder().getPriorityHours().get(priority);
        PriorityEnum priorityEnum = PriorityEnum.of(priority);
        int priorityHours = configHours != null ? configHours
                : (priorityEnum == null ? PriorityEnum.P3.getDefaultHours() : priorityEnum.getDefaultHours());
        int typeHours = faultTypeExpectHours == null ? priorityHours : faultTypeExpectHours;
        return Math.max(1, Math.min(priorityHours, typeHours));
    }
}
