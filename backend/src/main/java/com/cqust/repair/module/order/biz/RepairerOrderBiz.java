package com.cqust.repair.module.order.biz;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;

import java.util.List;

/**
 * 维修工端工单查询编排。
 *
 * @author school-repair
 */
public interface RepairerOrderBiz {

    /**
     * 抢单池（本楼栋 CREATED，按优先级 + 创建时间排序）。
     */
    List<OrderListItemVO> pool();

    /**
     * 我的工单列表。
     */
    PageResult<OrderListItemVO> myOrders(String status, long pageNum, long pageSize);

    /**
     * 工单详情（含图片、轨迹）。
     */
    OrderDetailVO detail(Long orderId);
}
