package com.cqust.repair.module.carousel.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.module.carousel.biz.CarouselBiz;
import com.cqust.repair.module.carousel.dto.CarouselCreateRequest;
import com.cqust.repair.module.carousel.dto.CarouselSortRequest;
import com.cqust.repair.module.carousel.dto.CarouselUpdateRequest;
import com.cqust.repair.module.carousel.dto.CarouselVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播管理接口（仅管理员）。
 *
 * @author school-repair
 */
@Tag(name = "首页轮播管理")
@RestController
@RequestMapping("/api/v1/manager/carousels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class CarouselAdminController {

    private final CarouselBiz carouselBiz;

    @Operation(summary = "轮播列表（含停用）")
    @GetMapping
    public R<List<CarouselVO>> list() {
        return R.success(carouselBiz.listAll());
    }

    @Operation(summary = "新增轮播")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CarouselCreateRequest request) {
        return R.success(carouselBiz.create(request));
    }

    @Operation(summary = "修改轮播")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CarouselUpdateRequest request) {
        carouselBiz.update(id, request);
        return R.success();
    }

    @Operation(summary = "删除轮播")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        carouselBiz.delete(id);
        return R.success();
    }

    @Operation(summary = "批量排序")
    @PutMapping("/sort")
    public R<Void> sort(@Valid @RequestBody CarouselSortRequest request) {
        carouselBiz.sort(request.getIds());
        return R.success();
    }
}
