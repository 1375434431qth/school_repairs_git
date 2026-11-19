package com.cqust.repair.module.order.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.order.biz.EvaluationBiz;
import com.cqust.repair.module.order.biz.OrderBiz;
import com.cqust.repair.module.order.dto.CreateOrderRequest;
import com.cqust.repair.module.order.dto.CreateOrderVO;
import com.cqust.repair.module.order.dto.EvaluationRequest;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.ReopenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端工单接口。
 *
 * @author school-repair
 */
@Tag(name = "学生端-工单")
@RestController
@RequestMapping("/api/v1/student/orders")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentOrderController {

    private final OrderBiz orderBiz;
    private final EvaluationBiz evaluationBiz;

    @Operation(summary = "提交报修")
    @PostMapping
    public R<CreateOrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
        return R.success(orderBiz.create(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "我的工单列表")
    @GetMapping
    public R<PageResult<OrderListItemVO>> list(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return R.success(orderBiz.myOrders(SecurityUtils.getCurrentUserId(), status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.success(orderBiz.detail(id));
    }

    @Operation(summary = "取消工单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        evaluationBiz.cancel(id, request);
        return R.success();
    }

    @Operation(summary = "确认完工")
    @PutMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        evaluationBiz.confirm(id);
        return R.success();
    }

    @Operation(summary = "提交评价")
    @PostMapping("/{id}/evaluate")
    public R<Void> evaluate(@PathVariable Long id, @Valid @RequestBody EvaluationRequest request) {
        evaluationBiz.evaluate(id, request);
        return R.success();
    }

    @Operation(summary = "反馈未修好（原单进入返修）")
    @PostMapping("/{id}/reopen")
    public R<CreateOrderVO> reopen(@PathVariable Long id, @Valid @RequestBody ReopenRequest request) {
        return R.success(evaluationBiz.reopen(id, request));
    }
}
