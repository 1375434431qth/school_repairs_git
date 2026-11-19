package com.cqust.repair.module.order.biz;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.module.order.dto.CreateOrderRequest;
import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;

/**
 * 学生报修用例编排。
 *
 * @author school-repair
 */
public interface OrderBiz {

    /**
     * 提交报修。
     */
    CreateOrderVO create(Long studentId, CreateOrderRequest request);

    /**
     * 我的工单列表。
     */
    PageResult<OrderListItemVO> myOrders(Long studentId, String status, String keyword,
                                        long pageNum, long pageSize);

    /**
     * 工单详情。
     */
    OrderDetailVO detail(Long orderId);
}
