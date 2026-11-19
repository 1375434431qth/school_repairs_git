package com.cqust.repair.module.order.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.infra.log.OpLog;
import com.cqust.repair.module.basedata.entity.FaultType;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.FaultTypeService;
import com.cqust.repair.module.notice.biz.NoticeBiz;
import com.cqust.repair.module.order.biz.DispatchBiz;
import com.cqust.repair.module.order.dto.AssignRequest;
import com.cqust.repair.module.order.dto.PriorityChangeRequest;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.TimeoutMarkRequest;
import com.cqust.repair.module.order.entity.TimeoutRecord;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.mapper.TimeoutRecordMapper;
import com.cqust.repair.module.order.service.DispatchService;
import com.cqust.repair.module.order.service.OrderCacheEvictor;
import com.cqust.repair.module.order.service.OrderRoundService;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.order.service.OrderTrackService;
import com.cqust.repair.module.order.service.TimeoutQueueService;
import com.cqust.repair.module.order.statemachine.OrderStateMachine;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 派单业务编排实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchBizImpl implements DispatchBiz {

    /** 抢单锁 TTL（秒） */
    private static final long GRAB_LOCK_SECONDS = 10;
    private static final Set<String> TIMEOUT_TYPES = Set.of("ACCEPT", "ARRIVE", "FINISH");

    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final OrderStateMachine orderStateMachine;
    private final OrderTrackService orderTrackService;
    private final OrderCacheEvictor orderCacheEvictor;
    private final TimeoutQueueService timeoutQueueService;
    private final UserService userService;
    private final BuildingService buildingService;
    private final FaultTypeService faultTypeService;
    private final OrderRoundService orderRoundService;
    private final TimeoutRecordMapper timeoutRecordMapper;
    private final RedisCacheService redisCacheService;
    private final NoticeBiz noticeBiz;

    @Override
    @OpLog(module = "ORDER", action = "ASSIGN")
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long managerId, Long orderId, AssignRequest request) {
        WorkOrder order = orderService.getRequired(orderId);
        assertManagerBuilding(managerId, order.getBuildingId());
        if (OrderStatusEnum.ASSIGNED.name().equals(order.getStatus())
                && request.getAssigneeId().equals(order.getAssigneeId())) {
            return;
        }
        if (!OrderStatusEnum.CREATED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅待派单工单可派单");
        }
        SysUser repairer = userService.getRequired(request.getAssigneeId());
        if (!RoleEnum.REPAIRER.name().equals(repairer.getRole())
                || repairer.getStatus() == null || repairer.getStatus() != 1) {
            throw new BizException(ResultCode.REPAIRER_UNAVAILABLE);
        }
        int roundNo = orderRoundService.currentRoundNo(order);
        if (OrderStatusEnum.REWORKING.name().equals(order.getStatus())) {
            // 返修中改派：只换人，不改状态
            LocalDateTime now = LocalDateTime.now();
            orderService.reassign(orderId, repairer.getId(), now);
            orderRoundService.markAssigned(orderId, roundNo, repairer.getId(), "ASSIGN", now);
            dispatchService.save(orderId, managerId, repairer.getId(), "ASSIGN",
                    order.getPriority(), order.getPriority(), request.getRemark(), roundNo);
            noticeBiz.send(repairer.getId(), orderId, NoticeTypeEnum.ORDER_ASSIGNED,
                    "返修工单已派给你", orderBrief(order) + "，请及时处理");
            return;
        }
        Map<String, Object> fields = new HashMap<>(4);
        LocalDateTime assignNow = LocalDateTime.now();
        fields.put("assigneeId", repairer.getId());
        fields.put("dispatchType", "ASSIGN");
        fields.put("assignTime", assignNow);
        String remark = request.getRemark() == null ? "管理员派单" : request.getRemark();
        orderStateMachine.transit(orderId, OrderStatusEnum.ASSIGNED, RoleEnum.MANAGER.name(),
                "ASSIGN", remark, fields);
        orderRoundService.markAssigned(orderId, roundNo, repairer.getId(), "ASSIGN", assignNow);
        dispatchService.save(orderId, managerId, repairer.getId(), "ASSIGN",
                order.getPriority(), order.getPriority(), request.getRemark(), roundNo);
        noticeBiz.send(repairer.getId(), orderId, NoticeTypeEnum.ORDER_ASSIGNED,
                "新工单已派给你", orderBrief(order) + "，请及时处理");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long orderId) {
        Long repairerId = SecurityUtils.getCurrentUserId();
        SysUser repairer = userService.getRequired(repairerId);
        if (!RoleEnum.REPAIRER.name().equals(repairer.getRole())
                || repairer.getStatus() == null || repairer.getStatus() != 1) {
            throw new BizException(ResultCode.REPAIRER_UNAVAILABLE);
        }
        WorkOrder order = orderService.getRequired(orderId);
        if (!OrderStatusEnum.CREATED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_ALREADY_TAKEN);
        }
        // BR-03：只能抢本楼栋工单
        if (repairer.getBuildingId() == null || !repairer.getBuildingId().equals(order.getBuildingId())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能抢本楼栋的工单");
        }
        // BR-02：同一工单拒单达 2 次后不再进入其抢单池
        if (dispatchService.listExcludedOrderIds(repairerId).contains(orderId)) {
            throw new BizException(ResultCode.FORBIDDEN, "该工单已拒绝两次，不可再抢");
        }
        String lockKey = RedisKeys.ORDER_GRAB_LOCK + orderId;
        String requestId = UUID.randomUUID().toString();
        if (!redisCacheService.setIfAbsent(lockKey, requestId, GRAB_LOCK_SECONDS)) {
            throw new BizException(ResultCode.ORDER_ALREADY_TAKEN);
        }
        try {
            int rows = dispatchService.grabUpdate(orderId, repairerId);
            if (rows == 0) {
                throw new BizException(ResultCode.ORDER_ALREADY_TAKEN);
            }
            dispatchService.save(orderId, repairerId, repairerId, "GRAB",
                    order.getPriority(), order.getPriority(), "维修工抢单", order.getCurrentRound());
            orderRoundService.markAssigned(orderId, orderRoundService.currentRoundNo(order),
                    repairerId, "GRAB", LocalDateTime.now());
            orderRoundService.markAccepted(orderId, orderRoundService.currentRoundNo(order), LocalDateTime.now());
            orderTrackService.record(orderId, order.getOrderNo(),
                    OrderStatusEnum.CREATED.name(), OrderStatusEnum.ACCEPTED.name(),
                    "GRAB", "抢单成功（视为接单）", order.getCurrentRound());
            orderCacheEvictor.evictAfterCommit(orderId);
            noticeBiz.send(repairerId, orderId, NoticeTypeEnum.ORDER_ASSIGNED,
                    "抢单成功", orderBrief(order) + "，请及时上门");
        } finally {
            redisCacheService.unlock(lockKey, requestId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId) {
        Long repairerId = SecurityUtils.getCurrentUserId();
        WorkOrder order = orderService.getRequired(orderId);
        if (!repairerId.equals(order.getAssigneeId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (OrderStatusEnum.ACCEPTED.name().equals(order.getStatus())) {
            // BR-16：重复接单幂等返回
            return;
        }
        if (!OrderStatusEnum.ASSIGNED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "当前状态不允许接单");
        }
        Map<String, Object> fields = new HashMap<>(2);
        fields.put("acceptTime", LocalDateTime.now());
        orderStateMachine.transit(orderId, OrderStatusEnum.ACCEPTED, RoleEnum.REPAIRER.name(),
                "ACCEPT", "维修工接单", fields);
        orderRoundService.markAccepted(orderId, orderRoundService.currentRoundNo(order), LocalDateTime.now());
        SysUser repairer = userService.findById(repairerId);
        String repairerName = repairer == null ? "维修师傅" : repairer.getRealName();
        noticeBiz.send(order.getStudentId(), orderId, NoticeTypeEnum.ORDER_ACCEPTED,
                "维修工已接单", repairerName + " 已接单，预计 30 分钟内上门");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, ReasonRequest request) {
        Long repairerId = SecurityUtils.getCurrentUserId();
        WorkOrder order = orderService.getRequired(orderId);
        if (!repairerId.equals(order.getAssigneeId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!OrderStatusEnum.ASSIGNED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已接单的工单请联系管理员释放");
        }
        dispatchService.clearAssignee(orderId);
        orderStateMachine.transit(orderId, OrderStatusEnum.CREATED, RoleEnum.REPAIRER.name(),
                "REJECT", "拒单：" + request.getReason(), Collections.emptyMap());
        orderRoundService.resetRound(orderId, orderRoundService.currentRoundNo(order));
        dispatchService.save(orderId, repairerId, repairerId, "REJECT",
                order.getPriority(), order.getPriority(), request.getReason(), order.getCurrentRound());
        SysUser repairer = userService.findById(repairerId);
        String repairerName = repairer == null ? "维修工" : repairer.getRealName();
        noticeBiz.sendToUsers(buildingService.listManagerIds(order.getBuildingId()), orderId,
                NoticeTypeEnum.ORDER_REJECTED, "维修工拒单",
                repairerName + " 拒单，工单 " + order.getOrderNo()
                        + " 已回到派单池，拒单理由：" + request.getReason().trim());
    }

    @Override
    @OpLog(module = "ORDER", action = "RELEASE")
    @Transactional(rollbackFor = Exception.class)
    public void release(Long managerId, Long orderId, ReasonRequest request) {
        WorkOrder order = orderService.getRequired(orderId);
        assertManagerBuilding(managerId, order.getBuildingId());
        boolean allowed = OrderStatusEnum.ACCEPTED.name().equals(order.getStatus())
                || OrderStatusEnum.ASSIGNED.name().equals(order.getStatus())
                || OrderStatusEnum.REWORKING.name().equals(order.getStatus());
        if (!allowed) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已派单、已接单或返修中的工单可释放");
        }
        Long formerAssignee = order.getAssigneeId();
        dispatchService.clearAssignee(orderId);
        orderStateMachine.transit(orderId, OrderStatusEnum.CREATED, RoleEnum.MANAGER.name(),
                "RELEASE", "管理员确认弃单：" + request.getReason(), Collections.emptyMap());
        orderRoundService.resetRound(orderId, orderRoundService.currentRoundNo(order));
        dispatchService.save(orderId, managerId, formerAssignee, "RELEASE",
                order.getPriority(), order.getPriority(), request.getReason(), order.getCurrentRound());
    }

    @Override
    @OpLog(module = "ORDER", action = "CHANGE_PRIORITY")
    @Transactional(rollbackFor = Exception.class)
    public void changePriority(Long managerId, Long orderId, PriorityChangeRequest request) {
        WorkOrder order = orderService.getRequired(orderId);
        assertManagerBuilding(managerId, order.getBuildingId());
        OrderStatusEnum current = OrderStatusEnum.of(order.getStatus());
        if (current == null || current.isTerminal()) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "终态工单不允许调整优先级");
        }
        if (order.getArriveTime() != null) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "已到场工单不允许调整优先级");
        }
        FaultType faultType = faultTypeService.getById(order.getFaultTypeId());
        int expectHours = orderService.calculateExpectHours(request.getPriority(),
                faultType == null ? null : faultType.getExpectHours());
        LocalDateTime deadline = order.getCreateTime().plusHours(expectHours);
        int rows = dispatchService.updatePriority(orderId, request.getPriority(), expectHours, deadline);
        if (rows == 0) {
            throw new BizException(ResultCode.CONCURRENT_MODIFIED);
        }
        timeoutQueueService.remove(orderId);
        timeoutQueueService.schedule(orderId, deadline);
        dispatchService.save(orderId, managerId, order.getAssigneeId(), "CHANGE_PRIORITY",
                order.getPriority(), request.getPriority(), request.getReason(), order.getCurrentRound());
        orderCacheEvictor.evictAfterCommit(orderId);
    }

    @Override
    @OpLog(module = "ORDER", action = "MARK_TIMEOUT")
    @Transactional(rollbackFor = Exception.class)
    public void markTimeout(Long managerId, Long orderId, TimeoutMarkRequest request) {
        WorkOrder order = orderService.getRequired(orderId);
        assertManagerBuilding(managerId, order.getBuildingId());
        String timeoutType = request.getTimeoutType().trim().toUpperCase(Locale.ROOT);
        if (!TIMEOUT_TYPES.contains(timeoutType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "超时环节取值 ACCEPT/ARRIVE/FINISH");
        }
        OrderStatusEnum current = OrderStatusEnum.of(order.getStatus());
        if (current == null || current.isTerminal()) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "终态工单不允许标记超时");
        }
        int roundNo = orderRoundService.currentRoundNo(order);
        Long exists = timeoutRecordMapper.selectCount(new QueryWrapper<TimeoutRecord>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .eq("timeout_type", timeoutType));
        if (exists != null && exists > 0) {
            // BR-16：同一轮同一环节重复标记幂等返回
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime planTime = order.getDeadlineTime() == null ? now : order.getDeadlineTime();
        TimeoutRecord record = new TimeoutRecord();
        record.setOrderId(orderId);
        record.setRoundNo(roundNo);
        record.setTimeoutType(timeoutType);
        record.setPlanTime(planTime);
        record.setActualTime(now);
        record.setOverMinutes((int) Math.max(0, Duration.between(planTime, now).toMinutes()));
        record.setMarkType("MANUAL");
        record.setMarkBy(managerId);
        record.setMarkTime(now);
        timeoutRecordMapper.insert(record);

        dispatchService.markTimeout(orderId, timeoutType);
        timeoutQueueService.remove(orderId);
        orderTrackService.record(orderId, order.getOrderNo(), order.getStatus(), order.getStatus(),
                "TIMEOUT", "管理员标记超时：" + timeoutType, roundNo);
        orderCacheEvictor.evictAfterCommit(orderId);
        List<Long> targets = new ArrayList<>(buildingService.listManagerIds(order.getBuildingId()));
        if (order.getAssigneeId() != null) {
            targets.add(order.getAssigneeId());
        }
        noticeBiz.sendToUsers(targets, orderId, NoticeTypeEnum.ORDER_TIMEOUT, "工单已超时",
                "工单 " + order.getOrderNo() + " 已超时 " + record.getOverMinutes() + " 分钟");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void urge(Long managerId, Long orderId) {
        WorkOrder order = orderService.getRequired(orderId);
        assertManagerBuilding(managerId, order.getBuildingId());
        OrderStatusEnum current = OrderStatusEnum.of(order.getStatus());
        if (current == null || current.isTerminal()) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "终态工单不可催单");
        }
        if (order.getAssigneeId() == null) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "工单尚未派单");
        }
        orderTrackService.record(orderId, order.getOrderNo(), order.getStatus(), order.getStatus(),
                "URGE", "管理员催单", order.getCurrentRound());
        noticeBiz.send(order.getAssigneeId(), orderId, NoticeTypeEnum.ORDER_URGED,
                "管理员催办", "管理员催办：工单 " + order.getOrderNo() + " 请尽快处理");
    }

    /**
     * 通知文案中的工单简述：工单号（房间 故障类型，Px）。
     */
    private String orderBrief(WorkOrder order) {
        return "工单 " + order.getOrderNo() + "（" + order.getRoomNo() + " "
                + order.getFaultTypeName() + "，P" + order.getPriority() + "）";
    }

    private void assertManagerBuilding(Long managerId, Long buildingId) {
        if (!buildingService.canManage(managerId, buildingId)) {
            throw new BizException(ResultCode.NO_PERMISSION_OF_BUILDING);
        }
    }
}
