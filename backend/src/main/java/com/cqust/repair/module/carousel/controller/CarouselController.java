package com.cqust.repair.module.carousel.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.module.carousel.biz.CarouselBiz;
import com.cqust.repair.module.carousel.dto.CarouselVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播展示接口（登录用户可见）。
 *
 * @author school-repair
 */
@Tag(name = "首页轮播")
@RestController
@RequestMapping("/api/v1/carousels")
@RequiredArgsConstructor
public class CarouselController {

    private final CarouselBiz carouselBiz;

    @Operation(summary = "首页轮播列表")
    @GetMapping
    public R<List<CarouselVO>> list() {
        return R.success(carouselBiz.listEnabled());
    }
}
