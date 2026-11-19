package com.cqust.repair.module.order.biz.impl;

import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.common.util.XssUtils;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.notice.biz.NoticeBiz;
import com.cqust.repair.module.order.biz.EvaluationBiz;
import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.EvaluationRequest;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.ReopenRequest;
import com.cqust.repair.module.order.entity.Evaluation;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.service.EvaluationService;
import com.cqust.repair.module.order.service.OrderRoundService;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.order.service.OrderTrackService;
import com.cqust.repair.module.order.service.TimeoutQueueService;
import com.cqust.repair.module.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端确认与评价编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class EvaluationBizImpl implements EvaluationBiz {

    private final OrderService orderService;
    private final EvaluationService evaluationService;
    private final OrderRoundService orderRoundService;
    private final OrderStateMachine orderStateMachine;
    private final OrderTrackService orderTrackService;
    private final TimeoutQueueService timeoutQueueService;
    private final BuildingService buildingService;
    private final NoticeBiz noticeBiz;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, ReasonRequest request) {
        WorkOrder order = loadOwnOrder(orderId);
        boolean allowed = OrderStatusEnum.CREATED.name().equals(order.getStatus())
                || OrderStatusEnum.ASSIGNED.name().equals(order.getStatus());
        if (!allowed) {
            throw new BizException(ResultCode.CANCEL_NOT_ALLOWED);
        }
        Map<String, Object> fields = new HashMap<>(4);
        fields.put("cancelTime", LocalDateTime.now());
        fields.put("cancelReason", request.getReason());
        orderStateMachine.transit(orderId, OrderStatusEnum.CANCELLED, RoleEnum.STUDENT.name(),
                "CANCEL", "学生取消：" + request.getReason(), fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        WorkOrder order = loadOwnOrder(orderId);
        if (OrderStatusEnum.CONFIRMED.name().equals(order.getStatus())
                || OrderStatusEnum.CLOSED.name().equals(order.getStatus())) {
            // BR-16：重复确认幂等
            return;
        }
        if (!OrderStatusEnum.FINISHED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅待确认的工单可确认完工");
        }
        Map<String, Object> fields = new HashMap<>(2);
        fields.put("confirmTime", LocalDateTime.now());
        orderStateMachine.transit(orderId, OrderStatusEnum.CONFIRMED, RoleEnum.STUDENT.name(),
                "CONFIRM", "学生确认完工", fields);
        orderRoundService.passCurrentRound(order, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(Long orderId, EvaluationRequest request) {
        WorkOrder order = loadOwnOrder(orderId);
        if (order.getEvaluateFlag() != null && order.getEvaluateFlag() == 1) {
            throw new BizException(ResultCode.ALREADY_EVALUATED);
        }
        if (!OrderStatusEnum.CONFIRMED.name().equals(order.getStatus())) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅已确认完工的工单可评价");
        }
        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setFinalRoundNo(orderRoundService.currentRoundNo(order));
        evaluation.setStudentId(order.getStudentId());
        evaluation.setRepairerId(order.getAssigneeId());
        evaluation.setScore(request.getScore());
        evaluation.setTags(request.getTags() == null ? null : XssUtils.escape(request.getTags()));
        evaluation.setContent(request.getContent() == null ? null : XssUtils.escape(request.getContent()));
        evaluation.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        try {
            evaluationService.save(evaluation);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.ALREADY_EVALUATED);
        }
        Map<String, Object> fields = new HashMap<>(4);
        fields.put("closeTime", LocalDateTime.now());
        fields.put("evaluateFlag", 1);
        orderStateMachine.transit(orderId, OrderStatusEnum.CLOSED, RoleEnum.STUDENT.name(),
                "EVALUATE", "学生评价：" + request.getScore() + " 星", fields);
        noticeBiz.send(order.getAssigneeId(), orderId, NoticeTypeEnum.ORDER_EVALUATED,
                "收到新评价", "工单 " + order.getOrderNo() + " 收到 " + request.getScore() + " 星评价");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderVO reopen(Long orderId, ReopenRequest request) {
        WorkOrder order = loadOwnOrder(orderId);
        boolean allowed = OrderStatusEnum.FINISHED.name().equals(order.getStatus())
                || OrderStatusEnum.CLOSED.name().equals(order.getStatus());
        if (!allowed) {
            throw new BizException(ResultCode.STATUS_NOT_ALLOWED, "仅待确认或已完成的工单可申请返修");
        }
        if (order.getAssigneeId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "原工单无维修工，无法发起返修");
        }
        String reason = XssUtils.escape(request.getReason().trim());
        LocalDateTime now = LocalDateTime.now();
        int nextRound = orderRoundService.startRework(order, reason);
        int expectHours = order.getExpectHours() == null ? 24 : order.getExpectHours();
        LocalDateTime deadline = now.plusHours(expectHours);

        Map<String, Object> fields = new HashMap<>(8);
        fields.put("currentRound", nextRound);
        fields.put("assigneeId", order.getAssigneeId());
        fields.put("dispatchType", "ASSIGN");
        fields.put("assignTime", now);
        fields.put("deadlineTime", deadline);
        fields.put("timeoutFlag", 0);
        orderStateMachine.transit(orderId, OrderStatusEnum.REWORKING, RoleEnum.STUDENT.name(),
                "REOPEN", "学生反馈未修好：" + reason, fields);
        if (request.getImageFileIds() != null && !request.getImageFileIds().isEmpty()) {
            orderService.appendImages(orderId, "BEFORE", request.getImageFileIds(), nextRound);
        }
        timeoutQueueService.schedule(orderId, deadline);

        List<Long> targets = new ArrayList<>(buildingService.listManagerIds(order.getBuildingId()));
        targets.add(order.getAssigneeId());
        noticeBiz.sendToUsers(targets, orderId, NoticeTypeEnum.ORDER_REWORK, "生成返修任务",
                "工单 " + order.getOrderNo() + " 第 " + nextRound + " 轮返修：" + reason);

        CreateOrderVO vo = new CreateOrderVO();
        vo.setOrderId(orderId);
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(OrderStatusEnum.REWORKING.name());
        vo.setDeadlineTime(deadline);
        return vo;
    }

    /**
     * 加载工单并校验当前学生为报修人。
     */
    private WorkOrder loadOwnOrder(Long orderId) {
        Long studentId = SecurityUtils.getCurrentUserId();
        WorkOrder order = orderService.getRequired(orderId);
        if (!studentId.equals(order.getStudentId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return order;
    }
}
