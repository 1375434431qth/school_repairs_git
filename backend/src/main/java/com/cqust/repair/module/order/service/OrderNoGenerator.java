package com.cqust.repair.module.order.service;

import com.cqust.repair.config.AppProperties;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工单号生成器：Redis INCR + 日期前缀，格式 WR + yyyyMMdd + 6 位序列。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final long SEQUENCE_TTL_SECONDS = 172800;

    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;

    /**
     * 生成下一个工单号。Redis 异常时退化为随机序列，由唯一索引 + 重试兜底。
     */
    public String nextOrderNo() {
        String date = LocalDate.now().format(DATE_FORMAT);
        long sequence;
        try {
            sequence = redisCacheService.increment(RedisKeys.ORDER_NO + date, SEQUENCE_TTL_SECONDS);
        } catch (Exception e) {
            log.error("工单号序列生成失败，使用兜底序列: {}", e.getMessage());
            sequence = ThreadLocalRandom.current().nextLong(1, 999_999);
        }
        return appProperties.getOrder().getNoPrefix() + date + String.format("%06d", sequence);
    }
}
