package com.cqust.repair.module.order.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.module.order.biz.DispatchBiz;
import com.cqust.repair.module.order.biz.RepairBiz;
import com.cqust.repair.module.order.biz.RepairerOrderBiz;
import com.cqust.repair.module.order.dto.FinishRequest;
import com.cqust.repair.module.order.dto.OrderDetailVO;
import com.cqust.repair.module.order.dto.OrderListItemVO;
import com.cqust.repair.module.order.dto.ReasonRequest;
import com.cqust.repair.module.order.dto.RepairRecordRequest;
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

import java.util.List;

/**
 * 维修工端工单接口。
 *
 * @author school-repair
 */
@Tag(name = "维修工端-工单")
@RestController
@RequestMapping("/api/v1/repairer/orders")
@PreAuthorize("hasRole('REPAIRER')")
@RequiredArgsConstructor
public class RepairerOrderController {

    private final RepairerOrderBiz repairerOrderBiz;
    private final DispatchBiz dispatchBiz;
    private final RepairBiz repairBiz;

    @Operation(summary = "抢单池")
    @GetMapping("/pool")
    public R<List<OrderListItemVO>> pool() {
        return R.success(repairerOrderBiz.pool());
    }

    @Operation(summary = "抢单")
    @PutMapping("/{id}/grab")
    public R<Void> grab(@PathVariable Long id) {
        dispatchBiz.grab(id);
        return R.success();
    }

    @Operation(summary = "我的工单")
    @GetMapping
    public R<PageResult<OrderListItemVO>> list(@RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return R.success(repairerOrderBiz.myOrders(status, pageNum, pageSize));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.success(repairerOrderBiz.detail(id));
    }

    @Operation(summary = "接单")
    @PutMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        dispatchBiz.accept(id);
        return R.success();
    }

    @Operation(summary = "拒单")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        dispatchBiz.reject(id, request);
        return R.success();
    }

    @Operation(summary = "登记到场")
    @PutMapping("/{id}/arrive")
    public R<Void> arrive(@PathVariable Long id) {
        repairBiz.arrive(id);
        return R.success();
    }

    @Operation(summary = "保存维修过程与配件")
    @PostMapping("/{id}/repair-record")
    public R<Void> saveRepairRecord(@PathVariable Long id,
                                    @Valid @RequestBody RepairRecordRequest request) {
        repairBiz.saveRecord(id, request);
        return R.success();
    }

    @Operation(summary = "提交完工")
    @PutMapping("/{id}/finish")
    public R<Void> finish(@PathVariable Long id, @Valid @RequestBody FinishRequest request) {
        repairBiz.finish(id, request);
        return R.success();
    }
}
