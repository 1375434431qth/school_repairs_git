package com.cqust.repair.module.order.biz;

import com.cqust.repair.module.order.dto.AssignRequest;
import com.cqust.repair.module.order.dto.PriorityChangeRequest;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.TimeoutMarkRequest;

/**
 * 派单业务编排：派单 / 抢单 / 接单 / 拒单 / 释放 / 调优先级 / 手动超时 / 催单。
 *
 * @author school-repair
 */
public interface DispatchBiz {

    /**
     * 管理员派单（CREATED → ASSIGNED）。
     */
    void assign(Long managerId, Long orderId, AssignRequest request);

    /**
     * 维修工抢单（CREATED → ACCEPTED，Redis 锁 + DB CAS）。
     */
    void grab(Long orderId);

    /**
     * 维修工接单（ASSIGNED → ACCEPTED）。
     */
    void accept(Long orderId);

    /**
     * 维修工拒单（ASSIGNED → CREATED，清空派单人并留痕）。
     */
    void reject(Long orderId, ReasonRequest request);

    /**
     * 管理员确认弃单（ACCEPTED/ASSIGNED → CREATED）。
     */
    void release(Long managerId, Long orderId, ReasonRequest request);

    /**
     * 管理员调整优先级并重算截止时间。
     */
    void changePriority(Long managerId, Long orderId, PriorityChangeRequest request);

    /**
     * 管理员手动标记超时。
     */
    void markTimeout(Long managerId, Long orderId, TimeoutMarkRequest request);

    /**
     * 管理员催单。
     */
    void urge(Long managerId, Long orderId);
}
