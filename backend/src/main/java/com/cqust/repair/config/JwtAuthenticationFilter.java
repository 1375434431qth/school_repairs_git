package com.cqust.repair.config;

import com.cqust.repair.common.util.CurrentUser;
import com.cqust.repair.common.util.JwtUtils;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.infra.cache.RedisKeys;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Header → 校验 Redis 单端 token → 写入 SecurityContext。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            // ThreadLocal 必须清理，否则线程池复用会串号导致越权
            SecurityUtils.clear();
        }
    }

    /**
     * 校验 token 并写入上下文；任一环节失败仅视为未认证，交由 EntryPoint 返回 401。
     */
    private void authenticate(String token) {
        try {
            Claims claims = jwtUtils.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String cachedToken = stringRedisTemplate.opsForValue().get(RedisKeys.AUTH_TOKEN + userId);
            if (!token.equals(cachedToken)) {
                // 已在别处登录/登出/被禁用
                SecurityContextHolder.clearContext();
                return;
            }
            String username = claims.get("username", String.class);
            List<?> roleClaim = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            String role = null;
            if (roleClaim != null && !roleClaim.isEmpty()) {
                role = String.valueOf(roleClaim.get(0));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            SecurityUtils.set(new CurrentUser(userId, username, role));
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            SecurityUtils.clear();
        }
    }

    /**
     * 从请求头解析 Bearer token。
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(appProperties.getJwt().getHeader());
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = appProperties.getJwt().getPrefix();
        if (!header.startsWith(prefix)) {
            return null;
        }
        String token = header.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
