package com.cqust.repair.module.ai.service.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.ai.service.AiRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * AI 限流实现：Lua 保证首次 INCR 同时设置 TTL。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitServiceImpl implements AiRateLimitService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String LUA =
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return current;";

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA, Long.class);

    @Override
    public void check(Long userId, AppProperties.Ai ai) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        String minute = now.format(MINUTE_FORMAT);
        String day = now.format(DAY_FORMAT);
        checkKey(RedisKeys.AI_LIMIT_USER_MINUTE + userId + ":" + minute,
                ai.getRateLimit().getPerMinute(), 70);
        checkKey(RedisKeys.AI_LIMIT_USER_DAY + userId + ":" + day,
                ai.getRateLimit().getPerDay(), 25 * 3600);
    }

    private void checkKey(String key, int limit, long ttlSeconds) {
        try {
            Long count = stringRedisTemplate.execute(script,
                    Collections.singletonList(key), String.valueOf(ttlSeconds));
            if (count != null && count > limit) {
                throw new BizException(ResultCode.AI_RATE_LIMITED);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // Redis 异常不阻塞 AI 主流程，仅记录日志
            log.warn("AI 限流检查失败，放行本次请求: {}", e.getMessage());
        }
    }
}
