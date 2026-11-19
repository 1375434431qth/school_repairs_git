package com.cqust.repair.module.basedata.service;

import com.cqust.repair.module.basedata.dto.BuildingSaveRequest;
import com.cqust.repair.module.basedata.dto.FaultTypeSaveRequest;
import com.cqust.repair.module.basedata.dto.RoomSaveRequest;
import com.cqust.repair.module.basedata.entity.FaultType;

/**
 * 基础数据维护原子服务（增删改 + 字典缓存失效）。
 *
 * @author school-repair
 */
public interface BaseDataAdminService {

    /**
     * 楼栋名称是否重复。
     *
     * @param excludeId 修改时排除自身 id，新增传 null
     */
    boolean buildingNameExists(String name, Long excludeId);

    /**
     * 楼栋编号是否重复。
     */
    boolean buildingCodeExists(String code, Long excludeId);

    /**
     * 新增楼栋。
     */
    Long createBuilding(BuildingSaveRequest request);

    /**
     * 修改楼栋。
     */
    void updateBuilding(Long buildingId, BuildingSaveRequest request);

    /**
     * 逻辑删除楼栋（同时清除楼栋与房间字典缓存）。
     */
    void deleteBuilding(Long buildingId);

    /**
     * 同一楼栋下房间号是否重复。
     */
    boolean roomNoExists(Long buildingId, String roomNo, Long excludeId);

    /**
     * 新增房间。
     */
    Long createRoom(RoomSaveRequest request);

    /**
     * 修改房间（支持调整所属楼栋）。
     */
    void updateRoom(Long roomId, RoomSaveRequest request);

    /**
     * 逻辑删除房间。
     */
    void deleteRoom(Long roomId);

    /**
     * 按 id 查询故障类型，不存在返回 null。
     */
    FaultType findFaultType(Long faultTypeId);

    /**
     * 同一父级下故障类型名称是否重复。
     */
    boolean faultTypeNameExists(Long parentId, String name, Long excludeId);

    /**
     * 是否存在子类型。
     */
    boolean hasChildren(Long faultTypeId);

    /**
     * 新增故障类型。
     */
    Long createFaultType(FaultTypeSaveRequest request);

    /**
     * 修改故障类型。
     */
    void updateFaultType(Long faultTypeId, FaultTypeSaveRequest request);

    /**
     * 逻辑删除故障类型。
     */
    void deleteFaultType(Long faultTypeId);
}
