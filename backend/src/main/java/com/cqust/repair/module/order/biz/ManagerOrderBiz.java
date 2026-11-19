package com.cqust.repair.module.order.biz;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.module.order.dto.BoardVO;
import com.cqust.repair.module.order.dto.ManagerOrderQuery;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.dto.RepairerLoadVO;

import java.util.List;

/**
 * 管理员工单查询编排：列表 / 详情 / 看板 / 超时列表 / 维修工负载。
 *
 * @author school-repair
 */
public interface ManagerOrderBiz {

    /**
     * 工单列表（多条件 + 楼栋权限范围）。
     */
    PageResult<OrderListItemVO> pageOrders(Long managerId, ManagerOrderQuery query);

    /**
     * 工单详情（含派单记录、轨迹、图片）。
     */
    OrderDetailVO detail(Long managerId, Long orderId);

    /**
     * 看板数据。
     *
     * @param buildingId 指定楼栋；为空表示全部负责楼栋
     */
    BoardVO board(Long managerId, Long buildingId);

    /**
     * 超时工单列表。
     */
    PageResult<OrderListItemVO> timeoutList(Long managerId, ManagerOrderQuery query);

    /**
     * 维修工负载。
     */
    List<RepairerLoadVO> repairerLoad(Long managerId, Long buildingId);
}
