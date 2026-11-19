package com.cqust.repair.module.stat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 维修工个人绩效查询（只读，与管理员统计口径隔离）。
 *
 * @author school-repair
 */
@Mapper
public interface RepairerStatMapper {

    /**
     * 个人绩效：区间内接单数、完工数、平均维修时长、平均评分。
     *
     * @param repairerId 维修工 id
     * @param beginTime  左闭
     * @param endTime    右开
     */
    Map<String, Object> personal(@Param("repairerId") Long repairerId,
                                 @Param("beginTime") LocalDateTime beginTime,
                                 @Param("endTime") LocalDateTime endTime);
}
