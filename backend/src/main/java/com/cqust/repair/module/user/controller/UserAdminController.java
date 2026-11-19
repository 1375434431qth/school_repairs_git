package com.cqust.repair.module.user.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.module.user.biz.UserAdminBiz;
import com.cqust.repair.module.user.dto.UserAdminQuery;
import com.cqust.repair.module.user.dto.UserAdminVO;
import com.cqust.repair.module.user.dto.UserBuildingsRequest;
import com.cqust.repair.module.user.dto.UserCreateRequest;
import com.cqust.repair.module.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户维护接口（列表 / 新建 / 编辑 / 楼栋授权）。
 *
 * @author school-repair
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class UserAdminController {

    private final UserAdminBiz userAdminBiz;

    @Operation(summary = "用户分页列表")
    @GetMapping
    public R<PageResult<UserAdminVO>> page(UserAdminQuery query) {
        return R.success(userAdminBiz.page(query));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    public R<UserAdminVO> create(@Valid @RequestBody UserCreateRequest request) {
        return R.success(userAdminBiz.create(request));
    }

    @Operation(summary = "编辑用户基础信息")
    @PutMapping("/{id}")
    public R<UserAdminVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return R.success(userAdminBiz.update(id, request));
    }

    @Operation(summary = "查询管理员授权楼栋")
    @GetMapping("/{id}/buildings")
    public R<List<Long>> buildings(@PathVariable Long id) {
        return R.success(userAdminBiz.listBuildingIds(id));
    }

    @Operation(summary = "保存管理员授权楼栋（覆盖式）")
    @PutMapping("/{id}/buildings")
    public R<Void> saveBuildings(@PathVariable Long id, @Valid @RequestBody UserBuildingsRequest request) {
        userAdminBiz.saveBuildingIds(id, request.getBuildingIds());
        return R.success();
    }
}
