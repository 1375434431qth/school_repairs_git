package com.cqust.repair.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.auth.dto.UserInfoVO;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.basedata.service.RoomService;
import com.cqust.repair.module.user.dto.ProfileUpdateRequest;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.mapper.SysUserMapper;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户原子服务实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final BuildingService buildingService;
    private final RoomService roomService;
    private final RedisCacheService redisCacheService;

    @Override
    public SysUser findById(Long userId) {
        return userId == null ? null : sysUserMapper.selectById(userId);
    }

    @Override
    public SysUser getRequired(Long userId) {
        SysUser user = findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    public SysUser findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username.trim()));
    }

    @Override
    public UserInfoVO buildUserInfo(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setRole(user.getRole());
        vo.setBuildingId(user.getBuildingId());
        vo.setBuildingName(buildingService.findName(user.getBuildingId()));
        vo.setRoomId(user.getRoomId());
        vo.setRoomNo(roomService.findRoomNo(user.getRoomId()));
        vo.setAvatar(user.getAvatar());
        vo.setSkillTags(splitTags(user.getSkillTags()));
        return vo;
    }

    @Override
    public List<SysUser> listAvailableRepairers(Long buildingId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<SysUser>()
                .eq("role", RoleEnum.REPAIRER.name())
                .eq("status", 1)
                .orderByAsc("id");
        if (buildingId != null) {
            wrapper.eq("building_id", buildingId);
        }
        List<SysUser> list = sysUserMapper.selectList(wrapper);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public Map<Long, SysUser> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(ids);
        Map<Long, SysUser> result = new HashMap<>(users.size());
        for (SysUser user : users) {
            result.put(user.getId(), user);
        }
        return result;
    }

    @Override
    public List<Long> listIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();
        List<SysUser> users = sysUserMapper.selectList(new QueryWrapper<SysUser>()
                .select("id")
                .and(wrapper -> wrapper.like("username", kw).or().like("real_name", kw)));
        List<Long> ids = new ArrayList<>(users.size());
        for (SysUser user : users) {
            ids.add(user.getId());
        }
        return ids;
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        SysUser user = getRequired(userId);
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long targetUserId, Integer status, Long operatorId) {
        if (targetUserId.equals(operatorId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能修改自己的账号状态");
        }
        SysUser user = getRequired(targetUserId);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        if (status != null && status == 0) {
            // 禁用后强制下线：下次请求校验 Redis token 失败即 401
            redisCacheService.delete(RedisKeys.AUTH_TOKEN + targetUserId);
            redisCacheService.delete(RedisKeys.AUTH_REFRESH + targetUserId);
        }
    }

    @Override
    public String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 技能标签拆分：逗号分隔，去除空白项。
     */
    private List<String> splitTags(String skillTags) {
        if (!StringUtils.hasText(skillTags)) {
            return Collections.emptyList();
        }
        List<String> tags = new ArrayList<>();
        for (String tag : skillTags.split(",")) {
            if (StringUtils.hasText(tag)) {
                tags.add(tag.trim());
            }
        }
        return tags;
    }
}
