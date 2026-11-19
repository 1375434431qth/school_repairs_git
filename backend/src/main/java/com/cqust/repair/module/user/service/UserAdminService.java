package com.cqust.repair.module.user.service;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.module.user.dto.UserAdminQuery;
import com.cqust.repair.module.user.dto.UserAdminVO;
import com.cqust.repair.module.user.dto.UserCreateRequest;
import com.cqust.repair.module.user.dto.UserUpdateRequest;

import java.util.List;

/**
 * 管理端用户维护原子服务（分页、新建、编辑、楼栋授权）。
 *
 * @author school-repair
 */
public interface UserAdminService {

    /**
     * 用户分页列表。
     */
    PageResult<UserAdminVO> page(UserAdminQuery query);

    /**
     * 新建用户并写入楼栋授权（仅 MANAGER）。
     */
    UserAdminVO create(UserCreateRequest request);

    /**
     * 编辑用户基础信息，仅更新传入字段。
     */
    UserAdminVO update(Long userId, UserUpdateRequest request);

    /**
     * 查询管理员授权楼栋 id 列表。
     */
    List<Long> listBuildingIds(Long managerId);

    /**
     * 覆盖式保存管理员授权楼栋。
     */
    void replaceBuildingIds(Long managerId, List<Long> buildingIds);
}
