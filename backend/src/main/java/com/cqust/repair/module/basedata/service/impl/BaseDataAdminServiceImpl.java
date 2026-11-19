package com.cqust.repair.module.basedata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.basedata.dto.BuildingSaveRequest;
import com.cqust.repair.module.basedata.dto.FaultTypeSaveRequest;
import com.cqust.repair.module.basedata.dto.RoomSaveRequest;
import com.cqust.repair.module.basedata.entity.Building;
import com.cqust.repair.module.basedata.entity.FaultType;
import com.cqust.repair.module.basedata.entity.Room;
import com.cqust.repair.module.basedata.mapper.BuildingMapper;
import com.cqust.repair.module.basedata.mapper.FaultTypeMapper;
import com.cqust.repair.module.basedata.mapper.RoomMapper;
import com.cqust.repair.module.basedata.service.BaseDataAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基础数据维护原子服务实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class BaseDataAdminServiceImpl implements BaseDataAdminService {

    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final FaultTypeMapper faultTypeMapper;
    private final RedisCacheService redisCacheService;

    @Override
    public boolean buildingNameExists(String name, Long excludeId) {
        QueryWrapper<Building> wrapper = new QueryWrapper<Building>().eq("name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = buildingMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public boolean buildingCodeExists(String code, Long excludeId) {
        QueryWrapper<Building> wrapper = new QueryWrapper<Building>().eq("code", code);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = buildingMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public Long createBuilding(BuildingSaveRequest request) {
        Building building = new Building();
        apply(building, request);
        buildingMapper.insert(building);
        evictBuildingCaches(building.getId());
        return building.getId();
    }

    @Override
    public void updateBuilding(Long buildingId, BuildingSaveRequest request) {
        Building building = requireBuilding(buildingId);
        apply(building, request);
        buildingMapper.updateById(building);
        evictBuildingCaches(buildingId);
    }

    @Override
    public void deleteBuilding(Long buildingId) {
        requireBuilding(buildingId);
        buildingMapper.deleteById(buildingId);
        evictBuildingCaches(buildingId);
    }

    @Override
    public boolean roomNoExists(Long buildingId, String roomNo, Long excludeId) {
        QueryWrapper<Room> wrapper = new QueryWrapper<Room>()
                .eq("building_id", buildingId)
                .eq("room_no", roomNo);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = roomMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public Long createRoom(RoomSaveRequest request) {
        Room room = new Room();
        apply(room, request);
        roomMapper.insert(room);
        evictRoomCache(room.getBuildingId());
        return room.getId();
    }

    @Override
    public void updateRoom(Long roomId, RoomSaveRequest request) {
        Room room = requireRoom(roomId);
        Long oldBuildingId = room.getBuildingId();
        apply(room, request);
        roomMapper.updateById(room);
        evictRoomCache(oldBuildingId);
        evictRoomCache(room.getBuildingId());
    }

    @Override
    public void deleteRoom(Long roomId) {
        Room room = requireRoom(roomId);
        roomMapper.deleteById(roomId);
        evictRoomCache(room.getBuildingId());
    }

    @Override
    public FaultType findFaultType(Long faultTypeId) {
        return faultTypeId == null ? null : faultTypeMapper.selectById(faultTypeId);
    }

    @Override
    public boolean faultTypeNameExists(Long parentId, String name, Long excludeId) {
        QueryWrapper<FaultType> wrapper = new QueryWrapper<FaultType>()
                .eq("parent_id", parentId == null ? 0L : parentId)
                .eq("name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = faultTypeMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public boolean hasChildren(Long faultTypeId) {
        Long count = faultTypeMapper.selectCount(new QueryWrapper<FaultType>().eq("parent_id", faultTypeId));
        return count != null && count > 0;
    }

    @Override
    public Long createFaultType(FaultTypeSaveRequest request) {
        FaultType faultType = new FaultType();
        apply(faultType, request);
        faultTypeMapper.insert(faultType);
        evictFaultTypeCache();
        return faultType.getId();
    }

    @Override
    public void updateFaultType(Long faultTypeId, FaultTypeSaveRequest request) {
        FaultType faultType = findFaultType(faultTypeId);
        if (faultType == null) {
            throw new BizException(ResultCode.NOT_FOUND, "故障类型不存在");
        }
        apply(faultType, request);
        faultTypeMapper.updateById(faultType);
        evictFaultTypeCache();
    }

    @Override
    public void deleteFaultType(Long faultTypeId) {
        if (findFaultType(faultTypeId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "故障类型不存在");
        }
        faultTypeMapper.deleteById(faultTypeId);
        evictFaultTypeCache();
    }

    private void apply(Building building, BuildingSaveRequest request) {
        building.setName(request.getName().trim());
        building.setCode(trimToNull(request.getCode()));
        building.setArea(trimToNull(request.getArea()));
        building.setFloors(request.getFloors());
        building.setRemark(trimToNull(request.getRemark()));
    }

    private void apply(Room room, RoomSaveRequest request) {
        room.setBuildingId(request.getBuildingId());
        room.setRoomNo(request.getRoomNo().trim());
        room.setFloor(request.getFloor());
    }

    private void apply(FaultType faultType, FaultTypeSaveRequest request) {
        faultType.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        faultType.setName(request.getName().trim());
        faultType.setCategory(request.getCategory().trim());
        faultType.setDefaultPriority(request.getDefaultPriority() == null ? 3 : request.getDefaultPriority());
        faultType.setExpectHours(request.getExpectHours() == null ? 24 : request.getExpectHours());
        faultType.setSort(request.getSort() == null ? 0 : request.getSort());
        faultType.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Building requireBuilding(Long buildingId) {
        Building building = buildingId == null ? null : buildingMapper.selectById(buildingId);
        if (building == null) {
            throw new BizException(ResultCode.NOT_FOUND, "楼栋不存在");
        }
        return building;
    }

    private Room requireRoom(Long roomId) {
        Room room = roomId == null ? null : roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    private void evictBuildingCaches(Long buildingId) {
        redisCacheService.delete(RedisKeys.DICT_BUILDING_LIST);
        if (buildingId != null) {
            redisCacheService.delete(RedisKeys.DICT_ROOM_LIST + buildingId);
        }
    }

    private void evictRoomCache(Long buildingId) {
        if (buildingId != null) {
            redisCacheService.delete(RedisKeys.DICT_ROOM_LIST + buildingId);
        }
    }

    private void evictFaultTypeCache() {
        redisCacheService.delete(RedisKeys.DICT_FAULT_TREE);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
