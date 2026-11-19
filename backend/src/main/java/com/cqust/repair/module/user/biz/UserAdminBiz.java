package com.cqust.repair.module.user.biz;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.module.user.dto.UserAdminQuery;
import com.cqust.repair.module.user.dto.UserAdminVO;
import com.cqust.repair.module.user.dto.UserCreateRequest;
import com.cqust.repair.module.user.dto.UserUpdateRequest;

import java.util.List;

/**
 * 管理端用户维护编排：参数校验 + 角色规则 + 数据权限。
 *
 * @author school-repair
 */
public interface UserAdminBiz {

    /**
     * 用户分页列表。
     */
    PageResult<UserAdminVO> page(UserAdminQuery query);

    /**
     * 新建用户。
     */
    UserAdminVO create(UserCreateRequest request);

    /**
     * 编辑用户基础信息。
     */
    UserAdminVO update(Long userId, UserUpdateRequest request);

    /**
     * 查询管理员授权楼栋。
     */
    List<Long> listBuildingIds(Long userId);

    /**
     * 覆盖式保存管理员授权楼栋。
     */
    void saveBuildingIds(Long userId, List<Long> buildingIds);
}
