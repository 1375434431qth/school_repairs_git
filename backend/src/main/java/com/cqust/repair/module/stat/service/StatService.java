package com.cqust.repair.module.stat.service;

import com.cqust.repair.common.enums.FaultCategoryEnum;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.stat.dto.AvgDurationVO;
import com.cqust.repair.module.stat.dto.OverviewVO;
import com.cqust.repair.module.stat.dto.RepairerRankVO;
import com.cqust.repair.module.stat.dto.ReworkRateVO;
import com.cqust.repair.module.stat.dto.StatQuery;
import com.cqust.repair.module.stat.dto.TypeDistVO;
import com.cqust.repair.module.stat.mapper.StatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计聚合服务：SQL 聚合 + 结果组装 + Redis 缓存（10 分钟）。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private static final long CACHE_SECONDS = 600;
    private static final int RATIO_SCALE = 1000;
    private static final int RATE_SCALE = 10000;

    private final StatMapper statMapper;
    private final RedisCacheService redisCacheService;

    /**
     * 报修类型分布。
     */
    public TypeDistVO typeDist(StatQuery query) {
        String key = RedisKeys.STAT_TYPE_DIST + keyPart(query);
        TypeDistVO cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        List<Map<String, Object>> rows = statMapper.typeDistribution(query);
        long total = 0L;
        for (Map<String, Object> row : rows) {
            total += toLong(row.get("cnt"));
        }
        List<TypeDistVO.Item> items = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            String category = row.get("category") == null ? FaultCategoryEnum.OTHER.name()
                    : row.get("category").toString();
            long count = toLong(row.get("cnt"));
            TypeDistVO.Item item = new TypeDistVO.Item();
            item.setCategory(category);
            item.setCategoryName(FaultCategoryEnum.labelOf(category));
            item.setCount(count);
            item.setRatio(total == 0 ? 0.0 : round(count * 1.0 / total, RATIO_SCALE));
            items.add(item);
        }
        TypeDistVO vo = new TypeDistVO();
        vo.setTotal(total);
        vo.setItems(items);
        redisCacheService.set(key, vo, CACHE_SECONDS);
        return vo;
    }

    /**
     * 平均处理时长趋势。
     */
    public AvgDurationVO avgDuration(StatQuery query) {
        String key = RedisKeys.STAT_AVG_DURATION + keyPart(query) + ":" + query.getGroupBy();
        AvgDurationVO cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        List<Map<String, Object>> rows = statMapper.avgDuration(query);
        List<AvgDurationVO.Point> points = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            AvgDurationVO.Point point = new AvgDurationVO.Point();
            point.setDate(row.get("d") == null ? null : row.get("d").toString());
            point.setAvgMinutes(toLong(row.get("avgMinutes")));
            point.setOrderCount(toLong(row.get("orderCount")));
            points.add(point);
        }
        AvgDurationVO vo = new AvgDurationVO();
        vo.setPoints(points);
        redisCacheService.set(key, vo, CACHE_SECONDS);
        return vo;
    }

    /**
     * 返修率 + 返修房间 Top10。
     */
    public ReworkRateVO reworkRate(StatQuery query) {
        String key = RedisKeys.STAT_REWORK_RATE + keyPart(query);
        ReworkRateVO cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        int finished = statMapper.finishedCount(query);
        int rework = statMapper.reworkCount(query);
        List<ReworkRateVO.TopRoom> topRooms = new ArrayList<>();
        for (Map<String, Object> row : statMapper.reworkTopRooms(query)) {
            ReworkRateVO.TopRoom topRoom = new ReworkRateVO.TopRoom();
            topRoom.setBuildingName(row.get("buildingName") == null ? null : row.get("buildingName").toString());
            topRoom.setRoomNo(row.get("roomNo") == null ? null : row.get("roomNo").toString());
            topRoom.setReworkCount(toLong(row.get("reworkCount")));
            topRooms.add(topRoom);
        }
        ReworkRateVO vo = new ReworkRateVO();
        vo.setFinishedCount((long) finished);
        vo.setReworkCount((long) rework);
        vo.setRate(finished == 0 ? 0.0 : round(rework * 1.0 / finished, RATE_SCALE));
        vo.setTopRooms(topRooms);
        redisCacheService.set(key, vo, CACHE_SECONDS);
        return vo;
    }

    /**
     * 维修工绩效排行。
     */
    public List<RepairerRankVO> repairerRank(StatQuery query) {
        String key = RedisKeys.STAT_REPAIRER_RANK + keyPart(query);
        @SuppressWarnings("unchecked")
        List<RepairerRankVO> cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        List<RepairerRankVO> result = new ArrayList<>();
        for (Map<String, Object> row : statMapper.repairerRank(query)) {
            RepairerRankVO vo = new RepairerRankVO();
            vo.setRepairerId(toLong(row.get("repairerId")));
            vo.setRepairerName(row.get("repairerName") == null ? null : row.get("repairerName").toString());
            vo.setFinishCount(toLong(row.get("finishCount")));
            vo.setAvgMinutes(row.get("avgMinutes") == null ? null : toLong(row.get("avgMinutes")));
            vo.setAvgScore(toDouble(row.get("avgScore")));
            result.add(vo);
        }
        redisCacheService.set(key, result, CACHE_SECONDS);
        return result;
    }

    /**
     * 核心指标卡。
     */
    public OverviewVO overview(StatQuery query) {
        String key = RedisKeys.STAT_OVERVIEW + keyPart(query);
        OverviewVO cached = redisCacheService.get(key);
        if (cached != null) {
            return cached;
        }
        Map<String, Object> row = statMapper.overview(query);
        OverviewVO vo = new OverviewVO();
        if (row != null) {
            vo.setTotalOrders(toLong(row.get("totalOrders")));
            vo.setPendingCount(toLong(row.get("pendingCount")));
            vo.setTimeoutCount(toLong(row.get("timeoutCount")));
            vo.setTodayNewCount(toLong(row.get("todayNewCount")));
            vo.setAvgDurationMinutes(toLong(row.get("avgDurationMinutes")));
            long finished = toLong(row.get("finishedCount"));
            long rework = toLong(row.get("reworkCount"));
            vo.setReworkRate(finished == 0 ? 0.0 : round(rework * 1.0 / finished, RATE_SCALE));
        }
        redisCacheService.set(key, vo, CACHE_SECONDS);
        return vo;
    }

    /**
     * 清空统计缓存。
     */
    public void refresh() {
        redisCacheService.deleteByPrefix(RedisKeys.STAT_PREFIX);
    }

    private String keyPart(StatQuery query) {
        String begin = query.getBeginTime() == null ? "all" : query.getBeginTime().toString();
        String end = query.getEndTime() == null ? "all" : query.getEndTime().toString();
        String scope = query.getBuildingId() != null
                ? String.valueOf(query.getBuildingId())
                : String.valueOf(query.getBuildingIds());
        return begin + "_" + end + "_" + scope;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private double round(double value, int scale) {
        return Math.round(value * scale) / (double) scale;
    }
}
