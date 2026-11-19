package com.cqust.repair.module.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.basedata.dto.RoomVO;
import com.cqust.repair.module.basedata.entity.Room;
import com.cqust.repair.module.basedata.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间字典服务。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    /** 房间字典缓存 1 小时 */
    private static final long CACHE_SECONDS = 3600;

    private final RoomMapper roomMapper;
    private final BuildingService buildingService;
    private final RedisCacheService redisCacheService;

    /**
     * 某楼栋房间列表（Redis 缓存）。
     */
    public List<RoomVO> listByBuilding(Long buildingId) {
        buildingService.getById(buildingId);
        String key = RedisKeys.DICT_ROOM_LIST + buildingId;
        List<RoomVO> cached = redisCacheService.get(key);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        List<Room> rooms = roomMapper.selectList(new QueryWrapper<Room>()
                .eq("building_id", buildingId)
                .orderByAsc("floor")
                .orderByAsc("room_no"));
        List<RoomVO> result = new ArrayList<>(rooms.size());
        for (Room room : rooms) {
            RoomVO vo = new RoomVO();
            vo.setId(room.getId());
            vo.setBuildingId(room.getBuildingId());
            vo.setRoomNo(room.getRoomNo());
            vo.setFloor(room.getFloor());
            result.add(vo);
        }
        redisCacheService.set(key, result, CACHE_SECONDS);
        return result;
    }

    /**
     * 按 id 查询房间，不存在抛 404。
     */
    public Room getById(Long roomId) {
        Room room = roomId == null ? null : roomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    /**
     * 房间 id → 房间号，null 或不存在返回 null（容错场景，不抛异常）。
     */
    public String findRoomNo(Long roomId) {
        if (roomId == null) {
            return null;
        }
        Room room = roomMapper.selectById(roomId);
        return room == null ? null : room.getRoomNo();
    }

    /**
     * 校验房间属于指定楼栋（BR：报修创建时必须校验）。
     */
    public Room getByIdInBuilding(Long roomId, Long buildingId) {
        Room room = getById(roomId);
        if (!room.getBuildingId().equals(buildingId)) {
            throw new BizException(ResultCode.ROOM_BUILDING_MISMATCH);
        }
        return room;
    }
}
