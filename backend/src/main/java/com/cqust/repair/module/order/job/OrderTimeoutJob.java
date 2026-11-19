package com.cqust.repair.module.order.job;

import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.common.enums.OrderStatusEnum;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.notice.biz.NoticeBiz;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.order.service.TimeoutMarkService;
import com.cqust.repair.module.order.service.TimeoutQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 超时扫描定时任务：每 5 分钟扫描 Redis ZSet 中到期的工单并自动标记超时。
 *
 * @see <a href="docs/01-后端开发文档.md#53-超时扫描机制">开发文档 5.3</a>
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    /** 单轮最多处理条数 */
    private static final int SCAN_LIMIT = 200;

    private final TimeoutQueueService timeoutQueueService;
    private final TimeoutMarkService timeoutMarkService;
    private final OrderService orderService;
    private final BuildingService buildingService;
    private final NoticeBiz noticeBiz;

    /**
     * 每 5 分钟执行一次。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scan() {
        Set<String> dueIds;
        try {
            dueIds = timeoutQueueService.scanDue(SCAN_LIMIT);
        } catch (Exception e) {
            log.error("超时队列读取失败", e);
            return;
        }
        if (dueIds.isEmpty()) {
            return;
        }
        log.info("超时扫描开始，本次到期工单 {} 条", dueIds.size());
        for (String raw : dueIds) {
            Long orderId = parseOrderId(raw);
            if (orderId == null) {
                continue;
            }
            try {
                handle(orderId);
            } catch (Exception e) {
                // 单条失败不影响其余工单，保留 ZSet 成员下轮重试
                log.error("自动标记超时失败: orderId={}", orderId, e);
            }
        }
    }

    private void handle(Long orderId) {
        WorkOrder order = orderService.findById(orderId);
        if (order == null) {
            timeoutQueueService.remove(orderId);
            return;
        }
        String timeoutType = mapTimeoutType(order.getStatus());
        if (timeoutType == null) {
            // 终态或非活跃状态，不再参与超时判定
            timeoutQueueService.remove(orderId);
            return;
        }
        boolean marked = timeoutMarkService.markAuto(order, timeoutType);
        timeoutQueueService.remove(orderId);
        if (!marked) {
            // 该环节已标记过，避免重复通知
            return;
        }
        long overMinutes = order.getDeadlineTime() == null ? 0
                : Math.max(0, Duration.between(order.getDeadlineTime(), LocalDateTime.now()).toMinutes());
        List<Long> targets = new ArrayList<>(buildingService.listManagerIds(order.getBuildingId()));
        if (order.getAssigneeId() != null) {
            targets.add(order.getAssigneeId());
        }
        noticeBiz.sendToUsers(targets, orderId, NoticeTypeEnum.ORDER_TIMEOUT, "工单已超时",
                "工单 " + order.getOrderNo() + " 已超时 " + overMinutes + " 分钟");
    }

    /**
     * 按当前状态映射超时环节：CREATED/ASSIGNED→ACCEPT，ACCEPTED/REWORKING→ARRIVE，ARRIVED→FINISH。
     */
    private String mapTimeoutType(String status) {
        if (OrderStatusEnum.CREATED.name().equals(status)
                || OrderStatusEnum.ASSIGNED.name().equals(status)) {
            return "ACCEPT";
        }
        if (OrderStatusEnum.ACCEPTED.name().equals(status)
                || OrderStatusEnum.REWORKING.name().equals(status)) {
            return "ARRIVE";
        }
        if (OrderStatusEnum.ARRIVED.name().equals(status)) {
            return "FINISH";
        }
        return null;
    }

    private Long parseOrderId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            log.warn("超时队列存在非法成员: {}", raw);
            return null;
        }
    }
}
