package com.cqust.repair.module.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.enums.FaultCategoryEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.basedata.dto.FaultTypeNodeVO;
import com.cqust.repair.module.basedata.entity.FaultType;
import com.cqust.repair.module.basedata.mapper.FaultTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 故障类型字典服务（两级树 + Redis 缓存）。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class FaultTypeService {

    /** 故障类型树缓存 24 小时 */
    private static final long CACHE_SECONDS = 86400;

    private final FaultTypeMapper faultTypeMapper;
    private final RedisCacheService redisCacheService;

    /**
     * 故障类型树（仅启用项）。
     */
    public List<FaultTypeNodeVO> tree() {
        List<FaultTypeNodeVO> cached = redisCacheService.get(RedisKeys.DICT_FAULT_TREE);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<FaultType> all = faultTypeMapper.selectList(new QueryWrapper<FaultType>()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        Map<Long, FaultTypeNodeVO> nodeMap = new LinkedHashMap<>();
        for (FaultType item : all) {
            if (item.getParentId() != null && item.getParentId() == 0L) {
                nodeMap.put(item.getId(), toNode(item));
            }
        }
        for (FaultType item : all) {
            if (item.getParentId() != null && item.getParentId() != 0L) {
                FaultTypeNodeVO parent = nodeMap.get(item.getParentId());
                if (parent != null) {
                    parent.getChildren().add(toNode(item));
                }
            }
        }
        List<FaultTypeNodeVO> result = new ArrayList<>(nodeMap.values());
        redisCacheService.set(RedisKeys.DICT_FAULT_TREE, result, CACHE_SECONDS);
        return result;
    }

    /**
     * 查询启用的故障类型，不存在或停用抛业务异常。
     */
    public FaultType getEnabledById(Long faultTypeId) {
        FaultType faultType = faultTypeId == null ? null : faultTypeMapper.selectById(faultTypeId);
        if (faultType == null || faultType.getStatus() == null || faultType.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "故障类型不存在或已停用");
        }
        return faultType;
    }

    /**
     * 按 id 查询，不校验启用状态（历史工单调优先级时仍需读取期望时长）。
     */
    public FaultType getById(Long faultTypeId) {
        return faultTypeId == null ? null : faultTypeMapper.selectById(faultTypeId);
    }

    /**
     * 平铺的启用故障类型（统计/下拉备用）。
     */
    public List<FaultType> listEnabled() {
        List<FaultType> list = faultTypeMapper.selectList(new QueryWrapper<FaultType>()
                .eq("status", 1)
                .orderByAsc("sort"));
        return list == null ? Collections.emptyList() : list;
    }

    private FaultTypeNodeVO toNode(FaultType item) {
        FaultTypeNodeVO node = new FaultTypeNodeVO();
        node.setId(item.getId());
        node.setParentId(item.getParentId());
        node.setName(item.getName());
        node.setCategory(item.getCategory());
        node.setCategoryName(FaultCategoryEnum.labelOf(item.getCategory()));
        node.setDefaultPriority(item.getDefaultPriority());
        node.setExpectHours(item.getExpectHours());
        node.setSort(item.getSort());
        return node;
    }
}
