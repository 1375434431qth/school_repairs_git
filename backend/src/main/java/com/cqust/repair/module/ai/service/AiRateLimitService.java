package com.cqust.repair.module.ai.service;

import com.cqust.repair.config.AppProperties;

/**
 * AI 限流服务：分钟级 + 日级。
 *
 * @author school-repair
 */
public interface AiRateLimitService {

    /**
     * 检查并计数；超限抛出 {@code AI_RATE_LIMITED}。
     */
    void check(Long userId, AppProperties.Ai ai);
}
