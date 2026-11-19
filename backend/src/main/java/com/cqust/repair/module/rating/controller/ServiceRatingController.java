package com.cqust.repair.module.rating.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.module.rating.dto.EvaluationPublicVO;
import com.cqust.repair.module.rating.dto.RepairerRatingVO;
import com.cqust.repair.module.rating.service.ServiceRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务评价公示接口（登录即可访问）：维修工平均星级榜 + 匿名评价明细。
 *
 * @author school-repair
 */
@Tag(name = "服务评价公示")
@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class ServiceRatingController {

    private final ServiceRatingService serviceRatingService;

    @Operation(summary = "维修工平均星级榜")
    @GetMapping("/repairers")
    public R<List<RepairerRatingVO>> repairerRatings() {
        return R.success(serviceRatingService.listRepairerRatings());
    }

    @Operation(summary = "匿名评价明细")
    @GetMapping("/evaluations")
    public R<PageResult<EvaluationPublicVO>> evaluations(
            @RequestParam(required = false) Long repairerId,
            @RequestParam(required = false) Integer score,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.success(serviceRatingService.pageEvaluations(repairerId, score, pageNum, pageSize));
    }
}
