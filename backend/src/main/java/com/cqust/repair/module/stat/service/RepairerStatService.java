package com.cqust.repair.module.stat.service;

import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.module.stat.dto.RepairerStatVO;
import com.cqust.repair.module.stat.mapper.RepairerStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 维修工个人绩效原子服务（Redis 缓存 5 分钟）。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairerStatService {

    /** 个人绩效缓存 5 分钟 */
    private static final long CACHE_SECONDS = 300;
    private static final String CACHE_PREFIX = "sr:stat:repairer:mine:";

    private final RepairerStatMapper repairerStatMapper;
    private final RedisCacheService redisCacheService;

    /**
     * 查询个人绩效，区间左闭右开（[beginDate, endDate+1)）。
     */
    public RepairerStatVO stat(Long repairerId, LocalDate beginDate, LocalDate endDate) {
        String key = CACHE_PREFIX + repairerId + ":" + beginDate + ":" + endDate;
        RepairerStatVO cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> row = repairerStatMapper.personal(repairerId,
                beginDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        RepairerStatVO vo = new RepairerStatVO();
        vo.setRepairerId(repairerId);
        vo.setBeginDate(beginDate);
        vo.setEndDate(endDate);
        vo.setAcceptCount(toLong(row, "acceptCount"));
        vo.setFinishCount(toLong(row, "finishCount"));
        vo.setAvgMinutes(toLong(row, "avgMinutes"));
        Object score = row == null ? null : row.get("avgScore");
        vo.setAvgScore(score == null ? null : ((Number) score).doubleValue());
        redisCacheService.set(key, vo, CACHE_SECONDS);
        return vo;
    }

    private Long toLong(Map<String, Object> row, String column) {
        Object value = row == null ? null : row.get(column);
        return value == null ? 0L : ((Number) value).longValue();
    }
}
