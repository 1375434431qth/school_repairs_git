package com.cqust.repair.module.user.biz.impl;

import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.RoomService;
import com.cqust.repair.module.user.biz.UserAdminBiz;
import com.cqust.repair.module.user.dto.UserAdminQuery;
import com.cqust.repair.module.user.dto.UserAdminVO;
import com.cqust.repair.module.user.dto.UserCreateRequest;
import com.cqust.repair.module.user.dto.UserUpdateRequest;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserAdminService;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端用户维护编排实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class UserAdminBizImpl implements UserAdminBiz {

    private final UserAdminService userAdminService;
    private final UserService userService;
    private final BuildingService buildingService;
    private final RoomService roomService;

    @Override
    public PageResult<UserAdminVO> page(UserAdminQuery query) {
        UserAdminQuery condition = query == null ? new UserAdminQuery() : query;
        condition.setRole(trimToNull(condition.getRole()));
        if (condition.getRole() != null) {
            parseRole(condition.getRole());
        }
        condition.setKeyword(trimToNull(condition.getKeyword()));
        return userAdminService.page(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO create(UserCreateRequest request) {
        String role = parseRole(request.getRole()).name();
        request.setRole(role);
        validateStatus(request.getStatus());
        validateProfile(role, request.getBuildingId(), request.getRoomId());
        List<Long> buildingIds = distinct(request.getManagedBuildingIds());
        validateBuildings(buildingIds);
        request.setManagedBuildingIds(buildingIds);
        return userAdminService.create(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO update(Long userId, UserUpdateRequest request) {
        SysUser user = userService.getRequired(userId);
        Long buildingId = request.getBuildingId() == null ? user.getBuildingId() : request.getBuildingId();
        Long roomId = request.getRoomId() == null ? user.getRoomId() : request.getRoomId();
        validateProfile(user.getRole(), buildingId, roomId);
        return userAdminService.update(userId, request);
    }

    @Override
    public List<Long> listBuildingIds(Long userId) {
        SysUser user = userService.getRequired(userId);
        if (!RoleEnum.MANAGER.name().equals(user.getRole())) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅管理员账号需要配置楼栋权限");
        }
        return userAdminService.listBuildingIds(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBuildingIds(Long userId, List<Long> buildingIds) {
        SysUser user = userService.getRequired(userId);
        if (!RoleEnum.MANAGER.name().equals(user.getRole())) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅管理员账号需要配置楼栋权限");
        }
        List<Long> distinctIds = distinct(buildingIds);
        validateBuildings(distinctIds);
        userAdminService.replaceBuildingIds(userId, distinctIds);
    }

    /**
     * 校验角色编码合法。
     */
    private RoleEnum parseRole(String role) {
        try {
            return RoleEnum.valueOf(role.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "角色只能是 STUDENT / REPAIRER / MANAGER");
        }
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态只能是 0（禁用）或 1（启用）");
        }
    }

    /**
     * 楼栋/房间归属校验 + 角色必填规则。
     */
    private void validateProfile(String role, Long buildingId, Long roomId) {
        if (buildingId != null) {
            buildingService.getById(buildingId);
        }
        if (roomId != null) {
            if (buildingId == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "指定房间时必须同时指定楼栋");
            }
            roomService.getByIdInBuilding(roomId, buildingId);
        }
        if (RoleEnum.STUDENT.name().equals(role) && (buildingId == null || roomId == null)) {
            throw new BizException(ResultCode.PARAM_ERROR, "学生账号必须指定楼栋与房间");
        }
        if (RoleEnum.REPAIRER.name().equals(role) && buildingId == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "维修工账号必须指定负责楼栋");
        }
    }

    private void validateBuildings(List<Long> buildingIds) {
        if (buildingIds == null) {
            return;
        }
        for (Long buildingId : buildingIds) {
            buildingService.getById(buildingId);
        }
    }

    private List<Long> distinct(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
