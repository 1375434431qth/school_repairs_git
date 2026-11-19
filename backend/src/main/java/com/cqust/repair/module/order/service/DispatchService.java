package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.module.order.entity.DispatchRecord;
import com.cqust.repair.module.order.mapper.DispatchMapper;
import com.cqust.repair.module.order.mapper.DispatchRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 派单原子服务：只做单表 SQL 与派单记录落库，不做编排。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class DispatchService {

    /** BR-02：同一维修工对同一工单累计拒单 2 次后不再推送 */
    private static final int REJECT_LIMIT = 2;

    private final DispatchMapper dispatchMapper;
    private final DispatchRecordMapper dispatchRecordMapper;

    /**
     * 抢单 CAS：CREATED 且无归属人才可抢。
     */
    public int grabUpdate(Long orderId, Long repairerId) {
        return dispatchMapper.grabUpdate(orderId, repairerId);
    }

    /**
     * 清空派单人（拒单 / 释放）。
     */
    public void clearAssignee(Long orderId) {
        dispatchMapper.clearAssignee(orderId);
    }

    /**
     * 调优先级并重算截止时间。
     *
     * @return 影响行数，0 表示已到场或终态
     */
    public int updatePriority(Long orderId, Integer priority, Integer expectHours, LocalDateTime deadline) {
        return dispatchMapper.updatePriority(orderId, priority, expectHours, deadline);
    }

    /**
     * 标记超时（只打标记，不改状态）。
     */
    public int markTimeout(Long orderId, String timeoutType) {
        return dispatchMapper.markTimeout(orderId, timeoutType);
    }

    /**
     * 写派单记录（指定维修轮次）。
     */
    public void save(Long orderId, Long operatorId, Long assigneeId, String dispatchType,
                     Integer beforePriority, Integer afterPriority, String remark, Integer roundNo) {
        DispatchRecord record = new DispatchRecord();
        record.setOrderId(orderId);
        record.setRoundNo(roundNo == null || roundNo < 1 ? 1 : roundNo);
        record.setOperatorId(operatorId);
        record.setAssigneeId(assigneeId);
        record.setDispatchType(dispatchType);
        record.setBeforePriority(beforePriority);
        record.setAfterPriority(afterPriority);
        record.setRemark(remark);
        dispatchRecordMapper.insert(record);
    }

    /**
     * 工单的派单记录（按时间正序）。
     */
    public List<DispatchRecord> listByOrder(Long orderId) {
        List<DispatchRecord> records = dispatchRecordMapper.selectList(new QueryWrapper<DispatchRecord>()
                .eq("order_id", orderId)
                .orderByAsc("create_time")
                .orderByAsc("id"));
        return records == null ? Collections.emptyList() : records;
    }

    /**
     * 该维修工已累计拒单达阈值的工单 id（不进入其抢单池）。
     */
    public List<Long> listExcludedOrderIds(Long repairerId) {
        List<Long> ids = dispatchRecordMapper.listRejectedOrderIds(repairerId, REJECT_LIMIT);
        return ids == null ? Collections.emptyList() : ids;
    }

    /**
     * 维修工负载统计原始结果（assigneeId / handCount / todayFinishCount）。
     */
    public List<Map<String, Object>> loadStats(Collection<Long> assigneeIds, LocalDateTime dayBegin) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return dispatchMapper.repairerLoad(assigneeIds, dayBegin);
    }
}
