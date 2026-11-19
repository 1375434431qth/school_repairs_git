package com.cqust.repair.module.order.biz;

import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.EvaluationRequest;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.ReopenRequest;

/**
 * 学生端确认与评价编排：取消、确认完工、评价、返修。
 *
 * @author school-repair
 */
public interface EvaluationBiz {

    /**
     * 取消工单（仅 CREATED/ASSIGNED）。
     */
    void cancel(Long orderId, ReasonRequest request);

    /**
     * 确认完工（FINISHED → CONFIRMED）。
     */
    void confirm(Long orderId);

    /**
     * 评价（CONFIRMED → CLOSED，一单一次）。
     */
    void evaluate(Long orderId, EvaluationRequest request);

    /**
     * 反馈未修好：原工单进入返修中（轮次 +1），必填原因。
     */
    CreateOrderVO reopen(Long orderId, ReopenRequest request);
}
