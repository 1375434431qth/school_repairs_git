package com.cqust.repair.module.basedata.biz.impl;

import com.cqust.repair.common.enums.FaultCategoryEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.basedata.biz.BaseDataAdminBiz;
import com.cqust.repair.module.basedata.dto.BuildingSaveRequest;
import com.cqust.repair.module.basedata.dto.FaultTypeSaveRequest;
import com.cqust.repair.module.basedata.dto.RoomSaveRequest;
import com.cqust.repair.module.basedata.entity.FaultType;
import com.cqust.repair.module.basedata.service.BaseDataAdminService;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 基础数据维护编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class BaseDataAdminBizImpl implements BaseDataAdminBiz {

    private final BaseDataAdminService baseDataAdminService;
    private final BuildingService buildingService;
    private final RoomService roomService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBuilding(BuildingSaveRequest request) {
        String name = request.getName().trim();
        if (baseDataAdminService.buildingNameExists(name, null)) {
            throw new BizException(ResultCode.PARAM_ERROR, "楼栋名称已存在");
        }
        String code = trimToNull(request.getCode());
        if (code != null && baseDataAdminService.buildingCodeExists(code, null)) {
            throw new BizException(ResultCode.PARAM_ERROR, "楼栋编号已存在");
        }
        return baseDataAdminService.createBuilding(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBuilding(Long buildingId, BuildingSaveRequest request) {
        buildingService.getById(buildingId);
        String name = request.getName().trim();
        if (baseDataAdminService.buildingNameExists(name, buildingId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "楼栋名称已存在");
        }
        String code = trimToNull(request.getCode());
        if (code != null && baseDataAdminService.buildingCodeExists(code, buildingId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "楼栋编号已存在");
        }
        baseDataAdminService.updateBuilding(buildingId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long buildingId) {
        buildingService.getById(buildingId);
        baseDataAdminService.deleteBuilding(buildingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoom(RoomSaveRequest request) {
        buildingService.getById(request.getBuildingId());
        String roomNo = request.getRoomNo().trim();
        if (baseDataAdminService.roomNoExists(request.getBuildingId(), roomNo, null)) {
            throw new BizException(ResultCode.PARAM_ERROR, "该楼栋下房间号已存在");
        }
        request.setFloor(resolveFloor(roomNo, request.getFloor()));
        return baseDataAdminService.createRoom(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(Long roomId, RoomSaveRequest request) {
        roomService.getById(roomId);
        buildingService.getById(request.getBuildingId());
        String roomNo = request.getRoomNo().trim();
        if (baseDataAdminService.roomNoExists(request.getBuildingId(), roomNo, roomId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "该楼栋下房间号已存在");
        }
        request.setFloor(resolveFloor(roomNo, request.getFloor()));
        baseDataAdminService.updateRoom(roomId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long roomId) {
        roomService.getById(roomId);
        baseDataAdminService.deleteRoom(roomId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFaultType(FaultTypeSaveRequest request) {
        validateFaultType(request, null);
        return baseDataAdminService.createFaultType(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFaultType(Long faultTypeId, FaultTypeSaveRequest request) {
        if (baseDataAdminService.findFaultType(faultTypeId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "故障类型不存在");
        }
        validateFaultType(request, faultTypeId);
        baseDataAdminService.updateFaultType(faultTypeId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaultType(Long faultTypeId) {
        if (baseDataAdminService.findFaultType(faultTypeId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "故障类型不存在");
        }
        if (baseDataAdminService.hasChildren(faultTypeId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先删除该分类下的子类型");
        }
        baseDataAdminService.deleteFaultType(faultTypeId);
    }

    /**
     * 故障类型校验：分类合法、两级树限制、同级名称唯一。
     */
    private void validateFaultType(FaultTypeSaveRequest request, Long excludeId) {
        String category = request.getCategory().trim();
        if (!categoryExists(category)) {
            throw new BizException(ResultCode.PARAM_ERROR, "故障分类不合法");
        }
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        if (excludeId != null && parentId.equals(excludeId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "父级分类不能是自己");
        }
        if (parentId != 0L) {
            FaultType parent = baseDataAdminService.findFaultType(parentId);
            if (parent == null) {
                throw new BizException(ResultCode.NOT_FOUND, "父级分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0L) {
                throw new BizException(ResultCode.PARAM_ERROR, "故障类型最多两级");
            }
            if (excludeId != null && baseDataAdminService.hasChildren(excludeId)) {
                throw new BizException(ResultCode.PARAM_ERROR, "该分类下存在子类型，不能调整为二级");
            }
        }
        String name = request.getName().trim();
        if (baseDataAdminService.faultTypeNameExists(parentId, name, excludeId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "同级下名称已存在");
        }
        Integer status = request.getStatus();
        if (status != null && status != 0 && status != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态只能是 0（停用）或 1（启用）");
        }
    }

    private boolean categoryExists(String category) {
        for (FaultCategoryEnum item : FaultCategoryEnum.values()) {
            if (item.name().equals(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 未填楼层时按房间号首位数字推断，推断失败按 1 层。
     */
    private Integer resolveFloor(String roomNo, Integer floor) {
        if (floor != null) {
            return floor;
        }
        char first = roomNo.charAt(0);
        return Character.isDigit(first) ? Character.getNumericValue(first) : 1;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
