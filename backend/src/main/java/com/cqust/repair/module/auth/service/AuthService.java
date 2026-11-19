package com.cqust.repair.module.auth.service;

import com.cqust.repair.module.user.entity.SysUser;

/**
 * 认证原子服务：账号校验、令牌签发与刷新校验。
 *
 * @author school-repair
 */
public interface AuthService {

    /**
     * 校验登录账号与密码（含登录失败限流、禁用校验）。
     *
     * @return 校验通过的用户
     */
    SysUser verify(String username, String rawPassword);

    /**
     * 签发 access + refresh token，并写入 Redis（单端登录）。
     */
    TokenPair issueTokens(SysUser user);

    /**
     * 校验 refresh token，返回所属用户 id。
     */
    Long verifyRefresh(String refreshToken);

    /**
     * 退出登录：删除 Redis 中的 access / refresh token。
     */
    void logout(Long userId);

    /**
     * 令牌对。
     *
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     */
    record TokenPair(String accessToken, String refreshToken) {
    }
}
