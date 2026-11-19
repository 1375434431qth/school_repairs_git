package com.cqust.repair.module.order.service;

import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * 超时扫描队列（Redis ZSet，score = 截止时间戳秒）。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutQueueService {

    private static final long ZSET_TTL_SECONDS = 30L * 24 * 3600;

    private final RedisCacheService redisCacheService;

    /**
     * 入队：工单进入下一环节时写入新的截止时间。
     */
    public void schedule(Long orderId, LocalDateTime deadline) {
        if (orderId == null || deadline == null) {
            return;
        }
        try {
            double score = deadline.atZone(ZoneId.systemDefault()).toEpochSecond();
            redisCacheService.zadd(RedisKeys.ORDER_TIMEOUT_ZSET, orderId, score);
            redisCacheService.expireZset(RedisKeys.ORDER_TIMEOUT_ZSET, ZSET_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("超时队列写入失败: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    /**
     * 出队：终态或环节完成时移除。
     */
    public void remove(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            redisCacheService.zrem(RedisKeys.ORDER_TIMEOUT_ZSET, orderId);
        } catch (Exception e) {
            log.warn("超时队列移除失败: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    /**
     * 取出已到期的工单 id（最多 limit 条）。
     */
    public Set<String> scanDue(int limit) {
        long now = System.currentTimeMillis() / 1000;
        return redisCacheService.zrangeByScore(RedisKeys.ORDER_TIMEOUT_ZSET, 0, now, 0, limit);
    }
}
