package com.cqust.repair.module.order.statemachine;

import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.mapper.WorkOrderMapper;
import com.cqust.repair.module.order.service.OrderCacheEvictor;
import com.cqust.repair.module.order.service.OrderTrackService;
import com.cqust.repair.module.order.service.TimeoutQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机：所有 status 变更的唯一入口。
 *
 * <p>职责：合法性校验 → 条件更新（status 做 CAS） → 轨迹 → 缓存失效 → 超时队列同步。
 * 禁止在 Service/Biz 中直接 updateById 改 status。</p>
 *
 * @author school-repair
 */
@Component
@RequiredArgsConstructor
public class OrderStateMachine {

    /** 进入这些状态后不再参与超时判定 */
    private static final Set<OrderStatusEnum> TIMEOUT_END = EnumSet.of(
            OrderStatusEnum.CONFIRMED, OrderStatusEnum.CLOSED, OrderStatusEnum.CANCELLED);

    private final WorkOrderMapper workOrderMapper;
    private final OrderTrackService orderTrackService;
    private final TimeoutQueueService timeoutQueueService;
    private final OrderCacheEvictor orderCacheEvictor;

    /**
     * 流转工单状态。
     *
     * @param orderId     工单 id
     * @param target      目标状态
     * @param role        操作人角色（ACCEPTED→CREATED 仅允许 MANAGER）
     * @param action      轨迹动作，如 ASSIGN / ACCEPT / ARRIVE / FINISH
     * @param remark      轨迹备注
     * @param extraFields 同时写入的字段（camelCase，禁止传 null 值）
     */
    public void transit(Long orderId, OrderStatusEnum target, String role, String action,
                        String remark, Map<String, Object> extraFields) {
        WorkOrder order = orderId == null ? null : workOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OrderStatusEnum current = OrderStatusEnum.of(order.getStatus());
        if (current == null || !current.canTransitTo(target)) {
            throw new BizException(ResultCode.ILLEGAL_STATUS_TRANSIT,
                    "当前状态[" + order.getStatus() + "]不允许流转到[" + target.name() + "]");
        }
        // 接单后弃单 / 返修释放只能由管理员确认（BR-15）
        if ((current == OrderStatusEnum.ACCEPTED || current == OrderStatusEnum.REWORKING)
                && target == OrderStatusEnum.CREATED
                && !RoleEnum.MANAGER.name().equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN, "维修工不可自行退单，请联系管理员");
        }
        int rows = workOrderMapper.transitUpdate(orderId, current.name(), target.name(),
                extraFields == null ? Collections.emptyMap() : extraFields);
        if (rows == 0) {
            throw new BizException(ResultCode.CONCURRENT_MODIFIED);
        }
        orderTrackService.record(orderId, order.getOrderNo(), current.name(), target.name(),
                action, remark, order.getCurrentRound());
        afterCommit(orderId, target);
    }

    /**
     * 事务提交后失效缓存并同步超时队列，避免回滚造成脏缓存。
     */
    private void afterCommit(Long orderId, OrderStatusEnum target) {
        orderCacheEvictor.evictAfterCommit(orderId);
        if (TIMEOUT_END.contains(target)) {
            timeoutQueueService.remove(orderId);
        }
    }
}
