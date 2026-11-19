package com.cqust.repair.module.order.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.order.biz.ManagerOrderBiz;
import com.cqust.repair.module.order.dto.RepairerLoadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 维修工负载查询（管理员派单参考）。
 *
 * @author school-repair
 */
@Tag(name = "管理员端-维修工负载")
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class RepairerQueryController {

    private final ManagerOrderBiz managerOrderBiz;

    @Operation(summary = "可派维修工列表（含在手工单数）")
    @GetMapping("/base/repairers")
    public R<List<RepairerLoadVO>> repairers(@RequestParam(required = false) Long buildingId) {
        return R.success(managerOrderBiz.repairerLoad(SecurityUtils.getCurrentUserId(), buildingId));
    }

    @Operation(summary = "维修工负载统计")
    @GetMapping("/manager/repairers/load")
    public R<List<RepairerLoadVO>> load(@RequestParam(required = false) Long buildingId) {
        return R.success(managerOrderBiz.repairerLoad(SecurityUtils.getCurrentUserId(), buildingId));
    }
}
