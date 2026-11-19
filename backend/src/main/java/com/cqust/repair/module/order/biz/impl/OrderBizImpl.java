package com.cqust.repair.module.order.biz.impl;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.order.biz.OrderBiz;
import com.cqust.repair.module.order.dto.CreateOrderRequest;
import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生报修用例编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class OrderBizImpl implements OrderBiz {

    private final OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderVO create(Long studentId, CreateOrderRequest request) {
        return orderService.create(studentId, request);
    }

    @Override
    public PageResult<OrderListItemVO> myOrders(Long studentId, String status, String keyword,
                                                long pageNum, long pageSize) {
        long safePageNum = Math.max(1, pageNum);
        long safePageSize = Math.min(100, Math.max(1, pageSize));
        return orderService.pageStudentOrders(studentId, status, keyword, safePageNum, safePageSize);
    }

    @Override
    public OrderDetailVO detail(Long orderId) {
        return orderService.detail(orderId, SecurityUtils.getCurrentUser());
    }
}
