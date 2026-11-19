package com.cqust.repair.module.stat.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.stat.biz.StatBiz;
import com.cqust.repair.module.stat.dto.AvgDurationVO;
import com.cqust.repair.module.stat.dto.OverviewVO;
import com.cqust.repair.module.stat.dto.RepairerRankVO;
import com.cqust.repair.module.stat.dto.ReworkRateVO;
import com.cqust.repair.module.stat.dto.TypeDistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理员统计接口。
 *
 * @author school-repair
 */
@Tag(name = "管理员端-统计")
@RestController
@RequestMapping("/api/v1/manager/stat")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class StatController {

    private final StatBiz statBiz;

    @Operation(summary = "核心指标卡")
    @GetMapping("/overview")
    public R<OverviewVO> overview() {
        return R.success(statBiz.overview(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "报修类型分布")
    @GetMapping("/type-dist")
    public R<TypeDistVO> typeDist(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId) {
        return R.success(statBiz.typeDist(SecurityUtils.getCurrentUserId(), beginDate, endDate, buildingId));
    }

    @Operation(summary = "平均处理时长趋势")
    @GetMapping("/avg-duration")
    public R<AvgDurationVO> avgDuration(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "day") String groupBy) {
        return R.success(statBiz.avgDuration(SecurityUtils.getCurrentUserId(), beginDate, endDate,
                buildingId, groupBy));
    }

    @Operation(summary = "返修率与返修房间 Top10")
    @GetMapping("/rework-rate")
    public R<ReworkRateVO> reworkRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId) {
        return R.success(statBiz.reworkRate(SecurityUtils.getCurrentUserId(), beginDate, endDate, buildingId));
    }

    @Operation(summary = "维修工绩效排行")
    @GetMapping("/repairer-rank")
    public R<List<RepairerRankVO>> repairerRank(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId) {
        return R.success(statBiz.repairerRank(SecurityUtils.getCurrentUserId(), beginDate, endDate, buildingId));
    }

    @Operation(summary = "强制刷新统计缓存")
    @PostMapping("/refresh")
    public R<Void> refresh() {
        statBiz.refresh();
        return R.success();
    }
}
