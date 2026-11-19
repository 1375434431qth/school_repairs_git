package com.cqust.repair.module.order.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.order.biz.DispatchBiz;
import com.cqust.repair.module.order.biz.ManagerOrderBiz;
import com.cqust.repair.module.order.dto.AssignRequest;
import com.cqust.repair.module.order.dto.BoardVO;
import com.cqust.repair.module.order.dto.ManagerOrderQuery;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.dto.PriorityChangeRequest;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.TimeoutMarkRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端工单接口。
 *
 * @author school-repair
 */
@Tag(name = "管理员端-工单")
@RestController
@RequestMapping("/api/v1/manager/orders")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerOrderController {

    private final ManagerOrderBiz managerOrderBiz;
    private final DispatchBiz dispatchBiz;

    @Operation(summary = "工单列表")
    @GetMapping
    public R<PageResult<OrderListItemVO>> list(ManagerOrderQuery query) {
        return R.success(managerOrderBiz.pageOrders(SecurityUtils.getCurrentUserId(), query));
    }

    @Operation(summary = "工单看板")
    @GetMapping("/board")
    public R<BoardVO> board(@RequestParam(required = false) Long buildingId) {
        return R.success(managerOrderBiz.board(SecurityUtils.getCurrentUserId(), buildingId));
    }

    @Operation(summary = "超时工单列表")
    @GetMapping("/timeout-list")
    public R<PageResult<OrderListItemVO>> timeoutList(ManagerOrderQuery query) {
        return R.success(managerOrderBiz.timeoutList(SecurityUtils.getCurrentUserId(), query));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.success(managerOrderBiz.detail(SecurityUtils.getCurrentUserId(), id));
    }

    @Operation(summary = "派单")
    @PutMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        dispatchBiz.assign(SecurityUtils.getCurrentUserId(), id, request);
        return R.success();
    }

    @Operation(summary = "调整优先级")
    @PutMapping("/{id}/priority")
    public R<Void> changePriority(@PathVariable Long id, @Valid @RequestBody PriorityChangeRequest request) {
        dispatchBiz.changePriority(SecurityUtils.getCurrentUserId(), id, request);
        return R.success();
    }

    @Operation(summary = "确认弃单")
    @PutMapping("/{id}/release")
    public R<Void> release(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        dispatchBiz.release(SecurityUtils.getCurrentUserId(), id, request);
        return R.success();
    }

    @Operation(summary = "手动标记超时")
    @PutMapping("/{id}/timeout")
    public R<Void> markTimeout(@PathVariable Long id, @Valid @RequestBody TimeoutMarkRequest request) {
        dispatchBiz.markTimeout(SecurityUtils.getCurrentUserId(), id, request);
        return R.success();
    }

    @Operation(summary = "催单")
    @PutMapping("/{id}/urge")
    public R<Void> urge(@PathVariable Long id) {
        dispatchBiz.urge(SecurityUtils.getCurrentUserId(), id);
        return R.success();
    }
}
