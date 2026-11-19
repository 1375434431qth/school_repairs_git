package com.cqust.repair.module.carousel.service;

import com.cqust.repair.module.carousel.dto.CarouselCreateRequest;
import com.cqust.repair.module.carousel.dto.CarouselUpdateRequest;
import com.cqust.repair.module.carousel.dto.CarouselVO;

import java.util.List;

/**
 * 首页轮播原子服务（含缓存与图片清理）。
 *
 * @author school-repair
 */
public interface CarouselService {

    /**
     * 首页展示：仅启用项，按 sort 升序，Redis 缓存 5 分钟。
     */
    List<CarouselVO> listEnabled();

    /**
     * 管理端列表：全部未删除项（含停用），按 sort 升序。
     */
    List<CarouselVO> listAll();

    /**
     * 新增轮播（最多 8 条），返回新 id。
     */
    Long create(CarouselCreateRequest request);

    /**
     * 修改标题 / 状态 / 顺序。
     */
    void update(Long id, CarouselUpdateRequest request);

    /**
     * 逻辑删除，同时清理 GridFS 图片。
     */
    void delete(Long id);

    /**
     * 按给定 id 顺序批量重排 sort=1..n。
     */
    void sort(List<Long> ids);
}
