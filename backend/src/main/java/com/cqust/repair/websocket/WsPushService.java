package com.cqust.repair.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * WebSocket 消息推送服务：推送失败只记日志，绝不影响业务事务。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsPushService {

    private final WsSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 推送给单个用户的所有在线会话。
     */
    public void push(Long userId, Object payload) {
        if (userId == null || payload == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionManager.getSessions(userId);
        if (sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("通知序列化失败: userId={}", userId, e);
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                log.warn("WebSocket 推送失败: userId={}, sessionId={}, error={}",
                        userId, session.getId(), e.getMessage());
            }
        }
    }

    /**
     * 批量推送给多个用户。
     */
    public void pushToUsers(Collection<Long> userIds, Object payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : new LinkedHashSet<>(userIds)) {
            push(userId, payload);
        }
    }
}
