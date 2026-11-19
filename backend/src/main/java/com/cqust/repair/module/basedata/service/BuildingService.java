package com.cqust.repair.module.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.basedata.dto.BuildingVO;
import com.cqust.repair.module.basedata.entity.Building;
import com.cqust.repair.module.basedata.entity.ManagerBuilding;
import com.cqust.repair.module.basedata.mapper.BuildingMapper;
import com.cqust.repair.module.basedata.mapper.ManagerBuildingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 楼栋字典服务（含管理员楼栋范围判定）。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class BuildingService {

    /** 楼栋字典缓存 1 小时 */
    private static final long CACHE_SECONDS = 3600;

    private final BuildingMapper buildingMapper;
    private final ManagerBuildingMapper managerBuildingMapper;
    private final RedisCacheService redisCacheService;

    /**
     * 楼栋列表（Redis 缓存）。
     */
    public List<BuildingVO> listAll() {
        List<BuildingVO> cached = redisCacheService.get(RedisKeys.DICT_BUILDING_LIST);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<Building> buildings = buildingMapper.selectList(new QueryWrapper<Building>().orderByAsc("id"));
        List<BuildingVO> result = new ArrayList<>(buildings.size());
        for (Building building : buildings) {
            result.add(toVO(building));
        }
        redisCacheService.set(RedisKeys.DICT_BUILDING_LIST, result, CACHE_SECONDS);
        return result;
    }

    /**
     * 按 id 查询楼栋，不存在抛 404。
     */
    public Building getById(Long buildingId) {
        Building building = buildingId == null ? null : buildingMapper.selectById(buildingId);
        if (building == null) {
            throw new BizException(ResultCode.NOT_FOUND, "楼栋不存在");
        }
        return building;
    }

    /**
     * 判断管理员是否有该楼栋权限（查 t_manager_building，不用 t_user.building_id）。
     */
    public boolean canManage(Long managerId, Long buildingId) {
        if (managerId == null || buildingId == null) {
            return false;
        }
        Long count = managerBuildingMapper.selectCount(new QueryWrapper<ManagerBuilding>()
                .eq("manager_id", managerId)
                .eq("building_id", buildingId));
        return count != null && count > 0;
    }

    /**
     * 管理员负责的楼栋 id 列表（一人可负责多栋）。
     */
    public List<Long> listManagedBuildingIds(Long managerId) {
        List<ManagerBuilding> relations = managerBuildingMapper.selectList(
                new QueryWrapper<ManagerBuilding>().eq("manager_id", managerId));
        return relations.stream().map(ManagerBuilding::getBuildingId).toList();
    }

    /**
     * 某楼栋的全部管理员 id（通知拒单/超时/返修用）。
     */
    public List<Long> listManagerIds(Long buildingId) {
        if (buildingId == null) {
            return List.of();
        }
        List<ManagerBuilding> relations = managerBuildingMapper.selectList(
                new QueryWrapper<ManagerBuilding>().eq("building_id", buildingId));
        return relations.stream().map(ManagerBuilding::getManagerId).distinct().toList();
    }

    /**
     * 楼栋 id → 名称，null 或不存在返回 null（用于登录信息等容错场景）。
     */
    public String findName(Long buildingId) {
        if (buildingId == null) {
            return null;
        }
        Building building = buildingMapper.selectById(buildingId);
        return building == null ? null : building.getName();
    }

    /**
     * 楼栋 id → 名称映射（详情/列表填冗余展示字段用）。
     */
    public Map<Long, String> nameMap() {
        Map<Long, String> result = new HashMap<>();
        for (Building building : buildingMapper.selectList(new QueryWrapper<Building>())) {
            result.put(building.getId(), building.getName());
        }
        return result;
    }

    private BuildingVO toVO(Building building) {
        BuildingVO vo = new BuildingVO();
        vo.setId(building.getId());
        vo.setName(building.getName());
        vo.setCode(building.getCode());
        vo.setArea(building.getArea());
        vo.setFloors(building.getFloors());
        return vo;
    }
}
