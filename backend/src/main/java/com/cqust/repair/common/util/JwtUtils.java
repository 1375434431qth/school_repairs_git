package com.cqust.repair.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.cqust.repair.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 签发与解析工具。
 *
 * @author school-repair
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_REFRESH = "refresh";

    private final AppProperties appProperties;

    private SecretKey signKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发访问令牌。
     *
     * @param userId   用户 id
     * @param username 登录账号
     * @param role     角色（不带 ROLE_ 前缀）
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        long expiryMillis = appProperties.getJwt().getExpireSeconds() * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, List.of(role))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(signKey())
                .compact();
    }

    /**
     * 生成刷新令牌（JWT，7 天，type=refresh，不可用于业务接口）。
     */
    public String generateRefreshToken(Long userId) {
        long expiryMillis = appProperties.getJwt().getRefreshExpireSeconds() * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(signKey())
                .compact();
    }

    /**
     * 解析刷新令牌，并校验类型必须为 refresh。
     */
    public Claims parseRefresh(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new io.jsonwebtoken.JwtException("非刷新令牌");
        }
        return claims;
    }

    /**
     * 解析并校验 JWT（签名与过期时间）。
     *
     * @param token JWT 字符串
     * @return 载荷
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
