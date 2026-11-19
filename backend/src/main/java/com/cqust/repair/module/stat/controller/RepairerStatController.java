package com.cqust.repair.module.stat.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.stat.biz.RepairerStatBiz;
import com.cqust.repair.module.stat.dto.RepairerStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 维修工个人绩效接口。
 *
 * @author school-repair
 */
@Tag(name = "维修工统计")
@RestController
@RequestMapping("/api/v1/repairer/stat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPAIRER')")
public class RepairerStatController {

    private final RepairerStatBiz repairerStatBiz;

    @Operation(summary = "我的绩效（默认本月）")
    @GetMapping("/mine")
    public R<RepairerStatVO> mine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(repairerStatBiz.mine(SecurityUtils.getCurrentUserId(), beginDate, endDate));
    }
}
