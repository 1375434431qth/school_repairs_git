package com.cqust.repair.websocket;

import com.cqust.repair.common.util.JwtUtils;
import com.cqust.repair.infra.cache.RedisKeys;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手鉴权：浏览器 WS 无法自定义 Header，token 只能走 query 参数。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            log.debug("WebSocket 握手缺少 token");
            return false;
        }
        try {
            Claims claims = jwtUtils.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String cached = stringRedisTemplate.opsForValue().get(RedisKeys.AUTH_TOKEN + userId);
            if (!token.equals(cached)) {
                log.debug("WebSocket 握手 token 已失效: userId={}", userId);
                return false;
            }
            attributes.put(NotificationHandler.ATTR_USER_ID, userId);
            attributes.put("username", claims.get("username", String.class));
            Object roles = claims.get("roles");
            if (roles instanceof List<?> list && !list.isEmpty()) {
                attributes.put("role", String.valueOf(list.get(0)));
            }
            return true;
        } catch (Exception e) {
            log.debug("WebSocket 握手鉴权失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
