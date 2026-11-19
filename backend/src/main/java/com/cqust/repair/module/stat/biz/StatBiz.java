package com.cqust.repair.module.stat.biz;

import com.cqust.repair.module.stat.dto.AvgDurationVO;
import com.cqust.repair.module.stat.dto.OverviewVO;
import com.cqust.repair.module.stat.dto.RepairerRankVO;
import com.cqust.repair.module.stat.dto.ReworkRateVO;
import com.cqust.repair.module.stat.dto.TypeDistVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理员统计编排：日期默认值、楼栋权限范围、参数校验。
 *
 * @author school-repair
 */
public interface StatBiz {

    OverviewVO overview(Long managerId);

    TypeDistVO typeDist(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId);

    AvgDurationVO avgDuration(Long managerId, LocalDate beginDate, LocalDate endDate,
                              Long buildingId, String groupBy);

    ReworkRateVO reworkRate(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId);

    List<RepairerRankVO> repairerRank(Long managerId, LocalDate beginDate, LocalDate endDate, Long buildingId);

    /**
     * 强制刷新统计缓存。
     */
    void refresh();
}
