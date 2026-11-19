package com.cqust.repair.module.order.service;

import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工单自动确认 / 自动关闭（BR-11）。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class OrderAutoService {

    /** 系统操作角色（不走用户权限校验） */
    private static final String SYSTEM_ROLE = "SYSTEM";

    private final OrderService orderService;
    private final OrderStateMachine orderStateMachine;
    private final AppProperties appProperties;

    /**
     * FINISHED 满 {@code auto-confirm-days} 天未确认 → 自动 CONFIRMED。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean autoConfirm(Long orderId) {
        WorkOrder order = orderService.findById(orderId);
        if (order == null || !OrderStatusEnum.FINISHED.name().equals(order.getStatus())) {
            return false;
        }
        Map<String, Object> fields = new HashMap<>(2);
        fields.put("confirmTime", LocalDateTime.now());
        orderStateMachine.transit(orderId, OrderStatusEnum.CONFIRMED, SYSTEM_ROLE,
                "AUTO_CONFIRM",
                "完工超过 " + appProperties.getOrder().getAutoConfirmDays() + " 天未确认，系统自动确认",
                fields);
        return true;
    }

    /**
     * CONFIRMED 满 {@code auto-close-days} 天未评价 → 自动 CLOSED。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean autoClose(Long orderId) {
        WorkOrder order = orderService.findById(orderId);
        if (order == null || !OrderStatusEnum.CONFIRMED.name().equals(order.getStatus())) {
            return false;
        }
        Map<String, Object> fields = new HashMap<>(2);
        fields.put("closeTime", LocalDateTime.now());
        orderStateMachine.transit(orderId, OrderStatusEnum.CLOSED, SYSTEM_ROLE,
                "AUTO_CLOSE",
                "评价窗口已满 " + appProperties.getOrder().getAutoCloseDays() + " 天，系统自动关闭",
                fields);
        return true;
    }
}
