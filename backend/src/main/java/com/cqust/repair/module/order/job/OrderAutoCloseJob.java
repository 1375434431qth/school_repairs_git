package com.cqust.repair.module.order.job;

import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.service.OrderAutoService;
import com.cqust.repair.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动确认 / 自动关闭定时任务（BR-11）：每日 00:10 执行。
 *
 * <p>FINISHED 满 auto-confirm-days 天未确认 → 自动 CONFIRMED；
 * CONFIRMED 满 auto-close-days 天未评价 → 自动 CLOSED。</p>
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCloseJob {

    private static final int BATCH_SIZE = 200;
    /** 单次调度最多处理轮数，防止异常数据导致死循环 */
    private static final int MAX_ROUNDS = 10;

    private final OrderService orderService;
    private final OrderAutoService orderAutoService;
    private final AppProperties appProperties;

    /**
     * 每日 00:10 执行。
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        int confirmDays = appProperties.getOrder().getAutoConfirmDays();
        int closeDays = appProperties.getOrder().getAutoCloseDays();
        autoConfirm(now.minusDays(confirmDays));
        autoClose(now.minusDays(closeDays));
    }

    private void autoConfirm(LocalDateTime before) {
        int handled = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<WorkOrder> orders = orderService.listFinishedBefore(before, BATCH_SIZE);
            if (orders.isEmpty()) {
                break;
            }
            for (WorkOrder order : orders) {
                try {
                    if (orderAutoService.autoConfirm(order.getId())) {
                        handled++;
                    }
                } catch (Exception e) {
                    log.error("自动确认失败: orderId={}", order.getId(), e);
                }
            }
            if (orders.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("自动确认完成，本轮处理 {} 条", handled);
    }

    private void autoClose(LocalDateTime before) {
        int handled = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<WorkOrder> orders = orderService.listConfirmedBefore(before, BATCH_SIZE);
            if (orders.isEmpty()) {
                break;
            }
            for (WorkOrder order : orders) {
                try {
                    if (orderAutoService.autoClose(order.getId())) {
                        handled++;
                    }
                } catch (Exception e) {
                    log.error("自动关闭失败: orderId={}", order.getId(), e);
                }
            }
            if (orders.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("自动关闭完成，本轮处理 {} 条", handled);
    }
}
