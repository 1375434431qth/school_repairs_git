package com.cqust.repair.module.carousel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.carousel.dto.CarouselCreateRequest;
import com.cqust.repair.module.carousel.dto.CarouselUpdateRequest;
import com.cqust.repair.module.carousel.dto.CarouselVO;
import com.cqust.repair.module.carousel.entity.Carousel;
import com.cqust.repair.module.carousel.mapper.CarouselMapper;
import com.cqust.repair.module.carousel.service.CarouselService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 首页轮播原子服务实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class CarouselServiceImpl implements CarouselService {

    private static final int MAX_COUNT = 8;
    private static final long CACHE_SECONDS = 300;

    private final CarouselMapper carouselMapper;
    private final RedisCacheService redisCacheService;
    private final GridFsTemplate gridFsTemplate;

    @Override
    public List<CarouselVO> listEnabled() {
        List<CarouselVO> cached = redisCacheService.get(RedisKeys.HOME_CAROUSEL);
        if (cached != null) {
            return cached;
        }
        List<Carousel> rows = carouselMapper.selectList(new QueryWrapper<Carousel>()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        List<CarouselVO> result = toVOList(rows);
        redisCacheService.set(RedisKeys.HOME_CAROUSEL, new ArrayList<>(result), CACHE_SECONDS);
        return result;
    }

    @Override
    public List<CarouselVO> listAll() {
        List<Carousel> rows = carouselMapper.selectList(new QueryWrapper<Carousel>()
                .orderByAsc("sort")
                .orderByAsc("id"));
        return toVOList(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CarouselCreateRequest request) {
        Long count = carouselMapper.selectCount(null);
        if (count != null && count >= MAX_COUNT) {
            throw new BizException(ResultCode.PARAM_ERROR, "轮播图最多 8 张");
        }
        Carousel entity = new Carousel();
        entity.setTitle(trimToNull(request.getTitle()));
        entity.setImageFileId(trimToNull(request.getImageFileId()));
        entity.setImageUrl(request.getImageUrl().trim());
        entity.setSort(request.getSort() == null ? nextSort() : request.getSort());
        entity.setStatus(1);
        carouselMapper.insert(entity);
        evictCache();
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CarouselUpdateRequest request) {
        Carousel entity = require(id);
        if (request.getTitle() != null) {
            entity.setTitle(trimToNull(request.getTitle()));
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getSort() != null) {
            entity.setSort(request.getSort());
        }
        carouselMapper.updateById(entity);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Carousel entity = require(id);
        carouselMapper.deleteById(id);
        if (StringUtils.hasText(entity.getImageFileId()) && ObjectId.isValid(entity.getImageFileId())) {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(entity.getImageFileId()))));
        }
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<Long> ids) {
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(ids));
        if (distinct.size() > MAX_COUNT) {
            throw new BizException(ResultCode.PARAM_ERROR, "轮播图最多 8 张");
        }
        for (int index = 0; index < distinct.size(); index++) {
            Carousel entity = require(distinct.get(index));
            entity.setSort(index + 1);
            carouselMapper.updateById(entity);
        }
        evictCache();
    }

    private Carousel require(Long id) {
        Carousel entity = id == null ? null : carouselMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "轮播图不存在");
        }
        return entity;
    }

    private int nextSort() {
        Carousel last = carouselMapper.selectOne(new QueryWrapper<Carousel>()
                .orderByDesc("sort")
                .last("LIMIT 1"));
        return last == null || last.getSort() == null ? 1 : last.getSort() + 1;
    }

    private void evictCache() {
        redisCacheService.delete(RedisKeys.HOME_CAROUSEL);
    }

    private List<CarouselVO> toVOList(List<Carousel> rows) {
        List<CarouselVO> result = new ArrayList<>(rows.size());
        for (Carousel row : rows) {
            CarouselVO vo = new CarouselVO();
            vo.setId(row.getId());
            vo.setTitle(row.getTitle());
            vo.setImageFileId(row.getImageFileId());
            vo.setImageUrl(row.getImageUrl());
            vo.setSort(row.getSort());
            vo.setStatus(row.getStatus());
            result.add(vo);
        }
        return result;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
