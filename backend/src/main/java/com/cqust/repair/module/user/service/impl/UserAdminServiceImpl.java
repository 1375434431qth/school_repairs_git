package com.cqust.repair.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.module.basedata.entity.ManagerBuilding;
import com.cqust.repair.module.basedata.entity.Room;
import com.cqust.repair.module.basedata.mapper.ManagerBuildingAdminMapper;
import com.cqust.repair.module.basedata.mapper.ManagerBuildingMapper;
import com.cqust.repair.module.basedata.mapper.RoomMapper;
import com.cqust.repair.module.basedata.service.BuildingService;
import com.cqust.repair.module.user.dto.UserAdminQuery;
import com.cqust.repair.module.user.dto.UserAdminVO;
import com.cqust.repair.module.user.dto.UserCreateRequest;
import com.cqust.repair.module.user.dto.UserUpdateRequest;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.mapper.SysUserMapper;
import com.cqust.repair.module.user.service.UserAdminService;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端用户维护原子服务实现。
 *
 * @author school-repair
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserMapper sysUserMapper;
    private final ManagerBuildingMapper managerBuildingMapper;
    private final ManagerBuildingAdminMapper managerBuildingAdminMapper;
    private final RoomMapper roomMapper;
    private final BuildingService buildingService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserAdminVO> page(UserAdminQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? 10L : Math.min(query.getPageSize(), MAX_PAGE_SIZE);

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(query.getRole())) {
            wrapper.eq("role", query.getRole().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        if (query.getBuildingId() != null) {
            wrapper.eq("building_id", query.getBuildingId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("phone", keyword));
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page, toVOList(page.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO create(UserCreateRequest request) {
        String username = request.getUsername().trim();
        Long exists = sysUserMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "登录账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole().trim());
        user.setBuildingId(request.getBuildingId());
        user.setRoomId(request.getRoomId());
        user.setSkillTags(joinTags(request.getSkillTags()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "登录账号已存在");
        }

        if (RoleEnum.MANAGER.name().equals(user.getRole()) && request.getManagedBuildingIds() != null) {
            replaceBuildingIds(user.getId(), request.getManagedBuildingIds());
        }
        return toVOList(List.of(user)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAdminVO update(Long userId, UserUpdateRequest request) {
        SysUser user = userService.getRequired(userId);
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBuildingId() != null) {
            user.setBuildingId(request.getBuildingId());
        }
        if (request.getRoomId() != null) {
            user.setRoomId(request.getRoomId());
        }
        if (request.getSkillTags() != null) {
            user.setSkillTags(joinTags(request.getSkillTags()));
        }
        sysUserMapper.updateById(user);
        return toVOList(List.of(user)).get(0);
    }

    @Override
    public List<Long> listBuildingIds(Long managerId) {
        return buildingService.listManagedBuildingIds(managerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBuildingIds(Long managerId, List<Long> buildingIds) {
        // 物理删除：uk_manager_building 未包含 deleted 列，逻辑删除会导致重新授权时唯一键冲突
        managerBuildingAdminMapper.deleteByManagerId(managerId);
        if (buildingIds == null || buildingIds.isEmpty()) {
            return;
        }
        Set<Long> distinctIds = new LinkedHashSet<>(buildingIds);
        for (Long buildingId : distinctIds) {
            ManagerBuilding relation = new ManagerBuilding();
            relation.setManagerId(managerId);
            relation.setBuildingId(buildingId);
            managerBuildingMapper.insert(relation);
        }
    }

    /**
     * 批量转换（楼栋名、房间号、管理员授权楼栋一次查出，避免逐条查询）。
     */
    private List<UserAdminVO> toVOList(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> buildingNames = buildingService.nameMap();
        Map<Long, String> roomNos = roomNoMap(users);
        Map<Long, List<Long>> managedMap = managedBuildingMap(users);

        List<UserAdminVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            UserAdminVO vo = new UserAdminVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(userService.maskPhone(user.getPhone()));
            vo.setRole(user.getRole());
            vo.setRoleLabel(roleLabel(user.getRole()));
            vo.setBuildingId(user.getBuildingId());
            vo.setBuildingName(buildingNames.get(user.getBuildingId()));
            vo.setRoomId(user.getRoomId());
            vo.setRoomNo(roomNos.get(user.getRoomId()));
            vo.setAvatar(user.getAvatar());
            vo.setSkillTags(splitTags(user.getSkillTags()));
            vo.setStatus(user.getStatus());
            vo.setManagedBuildingIds(managedMap.getOrDefault(user.getId(), Collections.emptyList()));
            vo.setCreateTime(user.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private Map<Long, String> roomNoMap(List<SysUser> users) {
        Set<Long> roomIds = new HashSet<>();
        for (SysUser user : users) {
            if (user.getRoomId() != null) {
                roomIds.add(user.getRoomId());
            }
        }
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>(roomIds.size());
        for (Room room : roomMapper.selectBatchIds(roomIds)) {
            result.put(room.getId(), room.getRoomNo());
        }
        return result;
    }

    private Map<Long, List<Long>> managedBuildingMap(List<SysUser> users) {
        List<Long> managerIds = users.stream()
                .filter(user -> RoleEnum.MANAGER.name().equals(user.getRole()))
                .map(SysUser::getId)
                .toList();
        if (managerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> result = new HashMap<>(managerIds.size());
        List<ManagerBuilding> relations = managerBuildingMapper.selectList(
                new QueryWrapper<ManagerBuilding>().in("manager_id", managerIds));
        for (ManagerBuilding relation : relations) {
            result.computeIfAbsent(relation.getManagerId(), key -> new ArrayList<>())
                    .add(relation.getBuildingId());
        }
        return result;
    }

    private String roleLabel(String role) {
        for (RoleEnum item : RoleEnum.values()) {
            if (item.name().equals(role)) {
                return item.getLabel();
            }
        }
        return role;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>(tags.size());
        for (String tag : tags) {
            if (StringUtils.hasText(tag)) {
                cleaned.add(tag.trim());
            }
        }
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

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
