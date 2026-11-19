package com.cqust.repair.module.stat.biz.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.stat.biz.StatBiz;
import com.cqust.repair.module.stat.dto.AvgDurationVO;
import com.cqust.repair.module.stat.dto.OverviewVO;
import com.cqust.repair.module.stat.dto.RepairerRankVO;
import com.cqust.repair.module.stat.dto.ReworkRateVO;
import com.cqust.repair.module.stat.dto.StatQuery;
import com.cqust.repair.module.stat.dto.TypeDistVO;
import com.cqust.repair.module.stat.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 管理员统计编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class StatBizImpl implements StatBiz {

    private static final int DEFAULT_DAYS = 29;
    private static final long MAX_RANGE_DAYS = 365;
    private static final Set<String> GROUP_BY = Set.of("day", "week", "month");

    private final StatService statService;
    private final BuildingService buildingService;

    @Override
    public OverviewVO overview(Long managerId) {
        StatQuery query = new StatQuery();
        applyScope(query, managerId, null);
        return statService.overview(query);
    }

    @Override
    public TypeDistVO typeDist(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId) {
        return statService.typeDist(buildQuery(managerId, beginDate, endDate, buildingId));
    }

    @Override
    public AvgDurationVO avgDuration(Long managerId, LocalDate beginDate, LocalDate endDate,
                                     Long buildingId, String groupBy) {
        StatQuery query = buildQuery(managerId, beginDate, endDate, buildingId);
        String normalized = groupBy == null ? "day" : groupBy.trim().toLowerCase(Locale.ROOT);
        if (!GROUP_BY.contains(normalized)) {
            throw new BizException(ResultCode.PARAM_ERROR, "groupBy 取值 day/week/month");
        }
        query.setGroupBy(normalized);
        return statService.avgDuration(query);
    }

    @Override
    public ReworkRateVO reworkRate(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId) {
        return statService.reworkRate(buildQuery(managerId, beginDate, endDate, buildingId));
    }

    @Override
    public List<RepairerRankVO> repairerRank(Long managerId, LocalDate beginDate, LocalDate endDate,
                                             Long buildingId) {
        return statService.repairerRank(buildQuery(managerId, beginDate, endDate, buildingId));
    }

    @Override
    public void refresh() {
        statService.refresh();
    }

    /**
     * 默认最近 30 天，转左闭右开；超过 365 天拒绝。
     */
    private StatQuery buildQuery(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate begin = beginDate == null ? end.minusDays(DEFAULT_DAYS) : beginDate;
        if (begin.isAfter(end)) {
            throw new BizException(ResultCode.ILLEGAL_DATE_RANGE, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(begin, end) > MAX_RANGE_DAYS) {
            throw new BizException(ResultCode.STAT_RANGE_TOO_LARGE);
        }
        StatQuery query = new StatQuery();
        query.setBeginTime(begin.atStartOfDay());
        query.setEndTime(end.plusDays(1).atStartOfDay());
        applyScope(query, managerId, buildingId);
        return query;
    }

    /**
     * 楼栋权限范围：显式传值时必须在授权范围内；未传时限制在授权楼栋集合。
     */
    private void applyScope(StatQuery query, Long managerId, Long buildingId) {
        List<Long> managed = buildingService.listManagedBuildingIds(managerId);
        if (buildingId != null) {
            if (!managed.contains(buildingId)) {
                throw new BizException(ResultCode.NO_PERMISSION_OF_BUILDING);
            }
            query.setBuildingId(buildingId);
            return;
        }
        query.setBuildingIds(managed.isEmpty() ? List.of(-1L) : managed);
    }
}
