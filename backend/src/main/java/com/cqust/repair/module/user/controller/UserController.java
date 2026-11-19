package com.cqust.repair.module.user.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.user.dto.ProfileUpdateRequest;
import com.cqust.repair.module.user.dto.UserStatusRequest;
import com.cqust.repair.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人资料、账号状态管理。
 *
 * @author school-repair
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        userService.updateProfile(SecurityUtils.getCurrentUserId(), request);
        return R.success();
    }

    @Operation(summary = "启用/禁用账号（管理员）")
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.getStatus(), SecurityUtils.getCurrentUserId());
        return R.success();
    }
}
