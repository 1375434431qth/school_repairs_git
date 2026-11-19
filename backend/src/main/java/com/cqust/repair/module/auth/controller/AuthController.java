package com.cqust.repair.module.auth.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.IpUtils;
import com.cqust.repair.module.auth.biz.AuthBiz;
import com.cqust.repair.module.auth.dto.LoginRequest;
import com.cqust.repair.module.auth.dto.LoginVO;
import com.cqust.repair.module.auth.dto.RefreshRequest;
import com.cqust.repair.module.auth.dto.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * @author school-repair
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthBiz authBiz;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return R.success(authBiz.login(request, IpUtils.getClientIp(servletRequest)));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        authBiz.logout();
        return R.success();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.success(authBiz.me());
    }

    @Operation(summary = "刷新 token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.success(authBiz.refresh(request.getRefreshToken()));
    }
}
