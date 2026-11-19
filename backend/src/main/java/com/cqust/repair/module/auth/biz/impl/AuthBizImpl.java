package com.cqust.repair.module.auth.biz.impl;

import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.auth.biz.AuthBiz;
import com.cqust.repair.module.auth.dto.LoginRequest;
import com.cqust.repair.module.auth.dto.LoginVO;
import com.cqust.repair.module.auth.dto.UserInfoVO;
import com.cqust.repair.module.auth.service.AuthService;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证用例编排实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthBizImpl implements AuthBiz {

    private final AuthService authService;
    private final UserService userService;
    private final AppProperties appProperties;

    @Override
    public LoginVO login(LoginRequest request, String ip) {
        SysUser user = authService.verify(request.getUsername(), request.getPassword());
        AuthService.TokenPair tokens = authService.issueTokens(user);
        log.info("用户登录成功: userId={}, username={}, role={}, ip={}",
                user.getId(), user.getUsername(), user.getRole(), ip);
        return buildLoginVO(user, tokens);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Long userId = authService.verifyRefresh(refreshToken);
        SysUser user = userService.getRequired(userId);
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new com.cqust.repair.common.exception.BizException(
                    com.cqust.repair.common.result.ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        AuthService.TokenPair tokens = authService.issueTokens(user);
        return buildLoginVO(user, tokens);
    }

    @Override
    public UserInfoVO me() {
        SysUser user = userService.getRequired(SecurityUtils.getCurrentUserId());
        return userService.buildUserInfo(user);
    }

    @Override
    public void logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        authService.logout(userId);
        log.info("用户退出登录: userId={}", userId);
    }

    private LoginVO buildLoginVO(SysUser user, AuthService.TokenPair tokens) {
        LoginVO vo = new LoginVO();
        vo.setToken(tokens.accessToken());
        vo.setExpiresIn(appProperties.getJwt().getExpireSeconds());
        vo.setRefreshToken(tokens.refreshToken());
        vo.setUser(userService.buildUserInfo(user));
        return vo;
    }
}
