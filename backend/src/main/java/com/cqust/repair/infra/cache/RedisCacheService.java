package com.cqust.repair.infra.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 统一封装：对象缓存走 JSON 序列化，计数/锁/ZSet 走字符串。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    /** 解锁 Lua：比对 requestId 后再删除，防止误删他人锁 */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 读取缓存对象。
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 写入缓存对象。
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param ttlSeconds 过期秒数（必须大于 0）
     */
    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 删除缓存。
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存。
     */
    public void delete(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * SETNX + TTL，用于幂等、限流与分布式锁。
     *
     * @return true 表示设置成功
     */
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 自增计数，首次自增时设置过期时间。
     *
     * @return 自增后的值
     */
    public long increment(String key, long ttlSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0L : count;
    }

    /**
     * 获取字符串值。
     */
    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入字符串值。
     */
    public void setString(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 按前缀删除（小数据量场景使用，禁止在生产大 key 空间频繁调用）。
     */
    public void deleteByPrefix(String prefix) {
        Set<String> keys = stringRedisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 分布式锁解锁：仅当锁值等于 requestId 时删除。
     */
    public void unlock(String key, String requestId) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), requestId);
    }

    /**
     * 写入超时扫描 ZSet。
     *
     * @param orderId 工单 id
     * @param score   截止时间戳（秒）
     */
    public void zadd(String key, Long orderId, double score) {
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(orderId), score);
    }

    /**
     * 移除 ZSet 成员。
     */
    public void zrem(String key, Long orderId) {
        stringRedisTemplate.opsForZSet().remove(key, String.valueOf(orderId));
    }

    /**
     * 按分数区间取成员。
     *
     * @return 工单 id 集合（按分数升序）
     */
    public Set<String> zrangeByScore(String key, double min, double max, long offset, long count) {
        Set<String> members = stringRedisTemplate.opsForZSet()
                .rangeByScore(key, min, max, offset, count);
        return members == null ? Collections.emptySet() : members;
    }

    /**
     * 设置 ZSet 过期时间（兜底，防止无成员时永久驻留）。
     */
    public void expireZset(String key, long ttlSeconds) {
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取 ZSet 成员的分数。
     */
    public Double zscore(String key, Long orderId) {
        ZSetOperations<String, String> ops = stringRedisTemplate.opsForZSet();
        return ops.score(key, String.valueOf(orderId));
    }
}
