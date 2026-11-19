package com.cqust.repair.module.carousel.biz.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.carousel.biz.CarouselBiz;
import com.cqust.repair.module.carousel.dto.CarouselCreateRequest;
import com.cqust.repair.module.carousel.dto.CarouselUpdateRequest;
import com.cqust.repair.module.carousel.dto.CarouselVO;
import com.cqust.repair.module.carousel.service.CarouselService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 首页轮播编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class CarouselBizImpl implements CarouselBiz {

    private final CarouselService carouselService;

    @Override
    public List<CarouselVO> listEnabled() {
        return carouselService.listEnabled();
    }

    @Override
    public List<CarouselVO> listAll() {
        return carouselService.listAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CarouselCreateRequest request) {
        String url = request.getImageUrl() == null ? null : request.getImageUrl().trim();
        if (!StringUtils.hasText(url)) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片地址不能为空");
        }
        if (!url.startsWith("/") && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片地址格式不正确");
        }
        request.setImageUrl(url);
        return carouselService.create(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CarouselUpdateRequest request) {
        carouselService.update(id, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        carouselService.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BizException(ResultCode.PARAM_ERROR, "ids 不能包含空值");
        }
        carouselService.sort(ids);
    }
}
