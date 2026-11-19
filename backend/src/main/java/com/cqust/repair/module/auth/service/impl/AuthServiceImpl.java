package com.cqust.repair.module.auth.service.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.JwtUtils;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.auth.service.AuthService;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证原子服务实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 连续失败 5 次锁定 10 分钟 */
    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOGIN_LOCK_SECONDS = 600;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;

    @Override
    public SysUser verify(String username, String rawPassword) {
        String account = username == null ? "" : username.trim();
        checkLoginLock(account);

        SysUser user = userService.findByUsername(account);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            recordLoginFailure(account);
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        redisCacheService.delete(RedisKeys.LIMIT_LOGIN + account);
        return user;
    }

    @Override
    public TokenPair issueTokens(SysUser user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());
        redisCacheService.setString(RedisKeys.AUTH_TOKEN + user.getId(), accessToken,
                appProperties.getJwt().getExpireSeconds());
        redisCacheService.setString(RedisKeys.AUTH_REFRESH + user.getId(), refreshToken,
                appProperties.getJwt().getRefreshExpireSeconds());
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public Long verifyRefresh(String refreshToken) {
        try {
            Claims claims = jwtUtils.parseRefresh(refreshToken);
            Long userId = Long.valueOf(claims.getSubject());
            String cached = redisCacheService.getString(RedisKeys.AUTH_REFRESH + userId);
            if (!refreshToken.equals(cached)) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            return userId;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 签名错误 / 过期 / 类型不符统一按未登录处理
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void logout(Long userId) {
        redisCacheService.delete(RedisKeys.AUTH_TOKEN + userId);
        redisCacheService.delete(RedisKeys.AUTH_REFRESH + userId);
    }

    /**
     * 登录失败达 5 次后锁定 10 分钟。
     */
    private void checkLoginLock(String username) {
        String count = redisCacheService.getString(RedisKeys.LIMIT_LOGIN + username);
        if (count != null && Integer.parseInt(count) >= MAX_LOGIN_FAIL) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    private void recordLoginFailure(String username) {
        long count = redisCacheService.increment(RedisKeys.LIMIT_LOGIN + username, LOGIN_LOCK_SECONDS);
        log.warn("登录失败: username={}, 连续失败次数={}", username, count);
    }
}
