package com.cqust.repair.module.basedata.biz;

import com.cqust.repair.module.basedata.dto.BuildingSaveRequest;
import com.cqust.repair.module.basedata.dto.FaultTypeSaveRequest;
import com.cqust.repair.module.basedata.dto.RoomSaveRequest;

/**
 * 基础数据维护编排：唯一性、层级、字典一致性校验。
 *
 * @author school-repair
 */
public interface BaseDataAdminBiz {

    /**
     * 新增楼栋。
     */
    Long createBuilding(BuildingSaveRequest request);

    /**
     * 修改楼栋。
     */
    void updateBuilding(Long buildingId, BuildingSaveRequest request);

    /**
     * 删除楼栋（逻辑删除）。
     */
    void deleteBuilding(Long buildingId);

    /**
     * 新增房间。
     */
    Long createRoom(RoomSaveRequest request);

    /**
     * 修改房间。
     */
    void updateRoom(Long roomId, RoomSaveRequest request);

    /**
     * 删除房间（逻辑删除）。
     */
    void deleteRoom(Long roomId);

    /**
     * 新增故障类型。
     */
    Long createFaultType(FaultTypeSaveRequest request);

    /**
     * 修改故障类型。
     */
    void updateFaultType(Long faultTypeId, FaultTypeSaveRequest request);

    /**
     * 删除故障类型（逻辑删除）。
     */
    void deleteFaultType(Long faultTypeId);
}
