package com.cqust.repair.module.user.service;

import com.cqust.repair.module.auth.dto.UserInfoVO;
import com.cqust.repair.module.user.dto.ProfileUpdateRequest;
import com.cqust.repair.module.user.entity.SysUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户原子服务。
 *
 * @author school-repair
 */
public interface UserService {

    /**
     * 按 id 查询用户，不存在返回 null。
     */
    SysUser findById(Long userId);

    /**
     * 按 id 查询用户，不存在抛 404。
     */
    SysUser getRequired(Long userId);

    /**
     * 按登录账号查询用户，不存在返回 null。
     */
    SysUser findByUsername(String username);

    /**
     * 组装当前用户信息 VO（含楼栋名、房间号、技能标签）。
     */
    UserInfoVO buildUserInfo(SysUser user);

    /**
     * 某楼栋可派维修工（启用状态）。
     *
     * @param buildingId 楼栋 id，null 表示不过滤
     */
    List<SysUser> listAvailableRepairers(Long buildingId);

    /**
     * 批量查询用户并转 Map。
     */
    Map<Long, SysUser> mapByIds(Collection<Long> ids);

    /**
     * 按姓名或登录账号模糊匹配用户 id（通知接收人筛选）。
     */
    List<Long> listIdsByKeyword(String keyword);

    /**
     * 修改当前用户资料（手机号、头像）。
     */
    void updateProfile(Long userId, ProfileUpdateRequest request);

    /**
     * 启用 / 禁用账号；禁用时强制下线。
     *
     * @param operatorId 操作人 id（用于禁止自我禁用）
     */
    void updateStatus(Long targetUserId, Integer status, Long operatorId);

    /**
     * 手机号脱敏：138****1111。
     */
    String maskPhone(String phone);
}
