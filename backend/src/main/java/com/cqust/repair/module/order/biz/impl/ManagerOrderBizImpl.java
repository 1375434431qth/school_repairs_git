package com.cqust.repair.module.order.biz.impl;

import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.CurrentUser;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.order.biz.ManagerOrderBiz;
import com.cqust.repair.module.order.dto.BoardVO;
import com.cqust.repair.module.order.dto.DispatchRecordVO;
import com.cqust.repair.module.order.dto.ManagerOrderQuery;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.dto.RepairerLoadVO;
import com.cqust.repair.module.order.entity.DispatchRecord;
import com.cqust.repair.module.order.service.BoardService;
import com.cqust.repair.module.order.service.DispatchService;
import com.cqust.repair.module.order.service.OrderService;
import com.cqust.repair.module.order.service.RepairerLoadService;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员工单查询编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class ManagerOrderBizImpl implements ManagerOrderBiz {

    private final OrderService orderService;
    private final BoardService boardService;
    private final DispatchService dispatchService;
    private final RepairerLoadService repairerLoadService;
    private final UserService userService;
    private final BuildingService buildingService;

    @Override
    public PageResult<OrderListItemVO> pageOrders(Long managerId, ManagerOrderQuery query) {
        return orderService.pageManagerOrders(query, buildingService.listManagedBuildingIds(managerId));
    }

    @Override
    public OrderDetailVO detail(Long managerId, Long orderId) {
        CurrentUser currentUser = new CurrentUser(managerId, null, RoleEnum.MANAGER.name());
        OrderDetailVO detail = orderService.detail(orderId, currentUser);
        detail.setDispatchRecords(convertDispatchRecords(dispatchService.listByOrder(orderId)));
        return detail;
    }

    @Override
    public BoardVO board(Long managerId, Long buildingId) {
        List<Long> managed = buildingService.listManagedBuildingIds(managerId);
        if (buildingId != null) {
            assertBuilding(managed, buildingId);
            return boardService.board(List.of(buildingId), String.valueOf(buildingId));
        }
        if (managed.isEmpty()) {
            return boardService.board(List.of(), "empty:" + managerId);
        }
        List<Long> sorted = managed.stream().sorted().toList();
        String keyPart = sorted.stream().map(String::valueOf).collect(Collectors.joining(","));
        return boardService.board(sorted, keyPart);
    }

    @Override
    public PageResult<OrderListItemVO> timeoutList(Long managerId, ManagerOrderQuery query) {
        return orderService.pageTimeoutOrders(query, buildingService.listManagedBuildingIds(managerId));
    }

    @Override
    public List<RepairerLoadVO> repairerLoad(Long managerId, Long buildingId) {
        List<Long> managed = buildingService.listManagedBuildingIds(managerId);
        if (buildingId != null) {
            assertBuilding(managed, buildingId);
            return repairerLoadService.load(buildingId, managed);
        }
        if (managed.isEmpty()) {
            return Collections.emptyList();
        }
        return repairerLoadService.load(null, managed);
    }

    private void assertBuilding(List<Long> managed, Long buildingId) {
        if (!managed.contains(buildingId)) {
            throw new BizException(ResultCode.NO_PERMISSION_OF_BUILDING);
        }
    }

    /**
     * 派单记录转 VO，操作人 / 被派维修工姓名批量填充。
     */
    private List<DispatchRecordVO> convertDispatchRecords(List<DispatchRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        for (DispatchRecord record : records) {
            if (record.getOperatorId() != null) {
                userIds.add(record.getOperatorId());
            }
            if (record.getAssigneeId() != null) {
                userIds.add(record.getAssigneeId());
            }
        }
        Map<Long, SysUser> userMap = userService.mapByIds(userIds);
        List<DispatchRecordVO> result = new ArrayList<>(records.size());
        for (DispatchRecord record : records) {
            DispatchRecordVO vo = new DispatchRecordVO();
            vo.setId(record.getId());
            vo.setDispatchType(record.getDispatchType());
            vo.setOperatorId(record.getOperatorId());
            SysUser operator = record.getOperatorId() == null ? null : userMap.get(record.getOperatorId());
            vo.setOperatorName(operator == null ? null : operator.getRealName());
            vo.setAssigneeId(record.getAssigneeId());
            SysUser assignee = record.getAssigneeId() == null ? null : userMap.get(record.getAssigneeId());
            vo.setAssigneeName(assignee == null ? null : assignee.getRealName());
            vo.setBeforePriority(record.getBeforePriority());
            vo.setAfterPriority(record.getAfterPriority());
            vo.setRemark(record.getRemark());
            vo.setCreateTime(record.getCreateTime());
            result.add(vo);
        }
        return result;
    }
}
