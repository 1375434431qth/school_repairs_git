package com.cqust.repair.module.order.biz.impl;

import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.common.util.XssUtils;
import com.cqust.repair.module.order.biz.RepairBiz;
import com.cqust.repair.module.notice.biz.NoticeBiz;
import com.cqust.repair.module.order.dto.FinishRequest;
import com.cqust.repair.module.order.dto.RepairRecordRequest;
import com.cqust.repair.module.order.entity.PartUsage;
import com.cqust.repair.module.order.entity.RepairRecord;
import com.cqust.repair.module.order.entity.RepairRound;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.service.OrderRoundService;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.order.service.OrderTrackService;
import com.cqust.repair.module.order.service.RepairService;
import com.cqust.repair.module.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修业务编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairBizImpl implements RepairBiz {

    private static final int MIN_PROCESS_LENGTH = 10;
    private static final int MAX_AFTER_IMAGES = 6;
    private static final int MAX_TOTAL_IMAGES = 12;

    private final OrderService orderService;
    private final RepairService repairService;
    private final OrderRoundService orderRoundService;
    private final OrderStateMachine orderStateMachine;
    private final OrderTrackService orderTrackService;
    private final NoticeBiz noticeBiz;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arrive(Long orderId) {
        WorkOrder order = loadOwnOrder(orderId);
        int roundNo = orderRoundService.currentRoundNo(order);
        RepairRound round = orderRoundService.currentRound(orderId, roundNo);
        if (round != null && round.getArriveTime() != null) {
            // BR-16：同一轮重复到场幂等，不覆盖首次时间、不重复写轨迹
            return;
        }
        if (!OrderStatusEnum.ACCEPTED.name().equals(order.getStatus())
                && !OrderStatusEnum.REWORKING.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "请先接单再登记到场");
        }
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> fields = new HashMap<>(2);
        fields.put("arriveTime", now);
        orderStateMachine.transit(orderId, OrderStatusEnum.ARRIVED, RoleEnum.REPAIRER.name(),
                "ARRIVE", "维修工到场", fields);
        orderRoundService.markArrived(orderId, roundNo, now);
        noticeBiz.send(order.getStudentId(), orderId, NoticeTypeEnum.ORDER_ARRIVED,
                "维修师傅已到场", "维修师傅已到场，正在处理中");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(Long orderId, RepairRecordRequest request) {
        Long repairerId = SecurityUtils.getCurrentUserId();
        WorkOrder order = loadOwnOrder(orderId);
        if (!OrderStatusEnum.ARRIVED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "请先登记到场再填写维修过程");
        }
        boolean noParts = Boolean.TRUE.equals(request.getNoPartsFlag());
        List<RepairRecordRequest.PartItem> items = request.getParts() == null
                ? new ArrayList<>() : request.getParts();
        if (noParts && !items.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "已选择无需更换配件，请删除配件明细");
        }
        List<PartUsage> parts = new ArrayList<>(items.size());
        for (RepairRecordRequest.PartItem item : items) {
            PartUsage part = new PartUsage();
            part.setPartName(XssUtils.escape(item.getPartName()));
            part.setQuantity(item.getQuantity());
            part.setUnit(item.getUnit());
            part.setUnitPrice(item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice());
            parts.add(part);
        }
        int roundNo = orderRoundService.currentRoundNo(order);
        repairService.saveRecord(orderId, roundNo, repairerId,
                XssUtils.escape(request.getProcessDesc()), noParts, parts);
        orderTrackService.record(orderId, order.getOrderNo(), order.getStatus(), order.getStatus(),
                "REPAIR_RECORD", "更新维修过程与配件", roundNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long orderId, FinishRequest request) {
        WorkOrder order = loadOwnOrder(orderId);
        String status = order.getStatus();
        if (OrderStatusEnum.FINISHED.name().equals(status)
                || OrderStatusEnum.CONFIRMED.name().equals(status)
                || OrderStatusEnum.CLOSED.name().equals(status)) {
            // BR-16：重复完工幂等
            return;
        }
        if (!OrderStatusEnum.ARRIVED.name().equals(status)) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "请先登记到场再提交完工");
        }
        int roundNo = orderRoundService.currentRoundNo(order);
        RepairRecord record = repairService.getByOrder(orderId, roundNo);
        if (record == null || !StringUtils.hasText(record.getProcessDesc())
                || record.getProcessDesc().trim().length() < MIN_PROCESS_LENGTH) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先填写维修过程（至少 10 字）");
        }
        boolean hasParts = repairService.hasParts(orderId, roundNo);
        boolean noParts = record.getNoPartsFlag() != null && record.getNoPartsFlag() == 1;
        if (!hasParts && !noParts) {
            throw new BizException(ResultCode.PARAM_ERROR, "请填写更换配件或确认无需更换配件");
        }
        List<String> fileIds = request.getAfterImageFileIds();
        int afterCount = orderService.countImages(orderId, "AFTER");
        int totalCount = orderService.countImages(orderId, null);
        if (afterCount + fileIds.size() > MAX_AFTER_IMAGES || totalCount + fileIds.size() > MAX_TOTAL_IMAGES) {
            throw new BizException(ResultCode.IMAGE_COUNT_EXCEED);
        }
        orderService.appendImages(orderId, "AFTER", fileIds);

        LocalDateTime now = LocalDateTime.now();
        int costMinutes = (int) Math.max(0, Duration.between(order.getArriveTime(), now).toMinutes());
        repairService.complete(orderId, roundNo, XssUtils.escape(request.getResultDesc()), costMinutes);

        Map<String, Object> fields = new HashMap<>(2);
        fields.put("finishTime", now);
        orderStateMachine.transit(orderId, OrderStatusEnum.FINISHED, RoleEnum.REPAIRER.name(),
                "FINISH", "维修完工，等待学生确认", fields);
        orderRoundService.markFinished(orderId, roundNo, now, XssUtils.escape(request.getResultDesc()));
        noticeBiz.send(order.getStudentId(), orderId, NoticeTypeEnum.ORDER_FINISHED,
                "维修已完成", "维修已完成，请确认并评价");
    }

    /**
     * 加载工单并校验当前维修工为承接人（BR-12）。
     */
    private WorkOrder loadOwnOrder(Long orderId) {
        Long repairerId = SecurityUtils.getCurrentUserId();
        WorkOrder order = orderService.getRequired(orderId);
        if (!repairerId.equals(order.getAssigneeId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return order;
    }
}
