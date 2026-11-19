package com.cqust.repair.module.order.service;

import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 工单缓存失效：当前在事务中时延迟到提交后执行，避免回滚造成脏缓存。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCacheEvictor {

    private final RedisCacheService redisCacheService;

    /**
     * 失效工单详情与看板缓存（有事务则提交后执行）。
     */
    public void evictAfterCommit(Long orderId) {
        Runnable task = () -> {
            try {
                redisCacheService.delete(RedisKeys.ORDER_DETAIL + orderId);
                redisCacheService.deleteByPrefix(RedisKeys.ORDER_BOARD);
            } catch (Exception e) {
                log.warn("工单缓存失效失败: orderId={}, error={}", orderId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
