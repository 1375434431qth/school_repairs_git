package com.cqust.repair.module.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 派单相关工单表原子 SQL（抢单 CAS、释放、调优先级、手动超时、维修工负载）。
 *
 * @author school-repair
 */
@Mapper
public interface DispatchMapper {

    /**
     * 抢单：CREATED → ACCEPTED 一步到位，assignee_id IS NULL 做并发兜底。
     *
     * @return 影响行数，0 表示已被他人抢走
     */
    int grabUpdate(@Param("orderId") Long orderId, @Param("repairerId") Long repairerId);

    /**
     * 清空派单人信息（拒单 / 释放前调用）。
     */
    int clearAssignee(@Param("orderId") Long orderId);

    /**
     * 调整优先级并重算截止时间；已到场或终态的工单不更新。
     *
     * @return 影响行数，0 表示当前状态不允许
     */
    int updatePriority(@Param("orderId") Long orderId,
                       @Param("priority") Integer priority,
                       @Param("expectHours") Integer expectHours,
                       @Param("deadlineTime") LocalDateTime deadlineTime);

    /**
     * 标记超时：只更新工单超时标记，不改状态。
     */
    int markTimeout(@Param("orderId") Long orderId, @Param("timeoutType") String timeoutType);

    /**
     * 维修工负载：在手单数 + 今日完工数。
     *
     * @param assigneeIds 维修工 id 集合（不可为空）
     * @param dayBegin    今日零点
     */
    List<Map<String, Object>> repairerLoad(@Param("assigneeIds") Collection<Long> assigneeIds,
                                           @Param("dayBegin") LocalDateTime dayBegin);
}
