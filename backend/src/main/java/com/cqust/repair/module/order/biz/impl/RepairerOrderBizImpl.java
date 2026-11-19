package com.cqust.repair.module.order.biz.impl;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.order.biz.RepairerOrderBiz;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.service.DispatchService;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 维修工端工单查询编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RepairerOrderBizImpl implements RepairerOrderBiz {

    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final UserService userService;

    @Override
    public List<OrderListItemVO> pool() {
        Long repairerId = SecurityUtils.getCurrentUserId();
        SysUser repairer = userService.getRequired(repairerId);
        return orderService.listPool(repairer.getBuildingId(),
                dispatchService.listExcludedOrderIds(repairerId));
    }

    @Override
    public PageResult<OrderListItemVO> myOrders(String status, long pageNum, long pageSize) {
        return orderService.pageRepairerOrders(SecurityUtils.getCurrentUserId(), status, pageNum, pageSize);
    }

    @Override
    public OrderDetailVO detail(Long orderId) {
        return orderService.detail(orderId, SecurityUtils.getCurrentUser());
    }
}
