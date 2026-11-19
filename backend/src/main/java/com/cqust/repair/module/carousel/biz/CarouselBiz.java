package com.cqust.repair.module.carousel.biz;

import com.cqust.repair.module.carousel.dto.CarouselCreateRequest;
import com.cqust.repair.module.carousel.dto.CarouselUpdateRequest;
import com.cqust.repair.module.carousel.dto.CarouselVO;

import java.util.List;

/**
 * 首页轮播编排：参数归一化与业务校验。
 *
 * @author school-repair
 */
public interface CarouselBiz {

    /**
     * 首页轮播（仅启用）。
     */
    List<CarouselVO> listEnabled();

    /**
     * 管理端轮播列表（含停用）。
     */
    List<CarouselVO> listAll();

    /**
     * 新增轮播。
     */
    Long create(CarouselCreateRequest request);

    /**
     * 修改轮播。
     */
    void update(Long id, CarouselUpdateRequest request);

    /**
     * 删除轮播。
     */
    void delete(Long id);

    /**
     * 批量排序。
     */
    void sort(List<Long> ids);
}
