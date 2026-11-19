package com.cqust.repair.module.stat.mapper;

import com.cqust.repair.module.stat.dto.StatQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计聚合 SQL（复杂聚合统一走 XML，不在 Java 内存聚合）。
 *
 * @author school-repair
 */
@Mapper
public interface StatMapper {

    /**
     * 报修类型分布（按工单冗余字段 fault_category 聚合）。
     */
    List<Map<String, Object>> typeDistribution(@Param("q") StatQuery query);

    /**
     * 平均处理时长趋势（按天/周/月）。
     */
    List<Map<String, Object>> avgDuration(@Param("q") StatQuery query);

    /**
     * 区间内完工工单数（返修率分母）。
     */
    int finishedCount(@Param("q") StatQuery query);

    /**
     * 区间内完工且为返修的工单数（返修率分子）。
     */
    int reworkCount(@Param("q") StatQuery query);

    /**
     * 返修房间 Top10。
     */
    List<Map<String, Object>> reworkTopRooms(@Param("q") StatQuery query);

    /**
     * 维修工绩效排行。
     */
    List<Map<String, Object>> repairerRank(@Param("q") StatQuery query);

    /**
     * 核心指标卡。
     */
    Map<String, Object> overview(@Param("q") StatQuery query);
}
