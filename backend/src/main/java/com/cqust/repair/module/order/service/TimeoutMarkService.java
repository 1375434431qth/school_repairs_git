package com.cqust.repair.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.module.order.entity.TimeoutRecord;
import com.cqust.repair.module.order.entity.WorkOrder;
import com.cqust.repair.module.order.mapper.TimeoutRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 超时自动标记：写超时记录 + 更新工单超时标记（定时任务使用）。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutMarkService {

    private final TimeoutRecordMapper timeoutRecordMapper;
    private final DispatchService dispatchService;
    private final OrderTrackService orderTrackService;
    private final OrderCacheEvictor orderCacheEvictor;

    /**
     * 自动标记超时。
     *
     * @param order       当前工单
     * @param timeoutType 超时环节 ACCEPT/ARRIVE/FINISH
     * @return true 表示本次新写入超时记录；false 表示该环节已记录过
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markAuto(WorkOrder order, String timeoutType) {
        Long orderId = order.getId();
        int roundNo = order.getCurrentRound() == null || order.getCurrentRound() < 1
                ? 1 : order.getCurrentRound();
        Long exists = timeoutRecordMapper.selectCount(new QueryWrapper<TimeoutRecord>()
                .eq("order_id", orderId)
                .eq("round_no", roundNo)
                .eq("timeout_type", timeoutType));
        if (exists != null && exists > 0) {
            // uk_order_round_type 保证同一轮同一环节只记一条
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime planTime = order.getDeadlineTime() == null ? now : order.getDeadlineTime();
        TimeoutRecord record = new TimeoutRecord();
        record.setOrderId(orderId);
        record.setRoundNo(roundNo);
        record.setTimeoutType(timeoutType);
        record.setPlanTime(planTime);
        record.setActualTime(now);
        record.setOverMinutes((int) Math.max(0, Duration.between(planTime, now).toMinutes()));
        record.setMarkType("AUTO");
        record.setMarkTime(now);
        try {
            timeoutRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            return false;
        }
        dispatchService.markTimeout(orderId, timeoutType);
        orderTrackService.record(orderId, order.getOrderNo(), order.getStatus(), order.getStatus(),
                "TIMEOUT", "系统自动标记超时：" + timeoutType, roundNo);
        orderCacheEvictor.evictAfterCommit(orderId);
        return true;
    }
}
