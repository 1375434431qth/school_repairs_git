package com.cqust.repair.config;

import com.cqust.repair.websocket.JwtHandshakeInterceptor;
import com.cqust.repair.websocket.NotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：/ws/notice 端点 + 握手鉴权。
 *
 * @author school-repair
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "repair.notice.websocket", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationHandler notificationHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notice")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
