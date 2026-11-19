package com.cqust.repair.websocket;

import com.cqust.repair.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理：userId → 会话集合，单用户最多 {@code max-sessions-per-user} 个连接。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsSessionManager {

    private final AppProperties appProperties;

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话；超过上限时关闭最早建立的会话（多标签页场景）。
     */
    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            closeQuietly(session);
            return;
        }
        Set<WebSocketSession> userSessions = sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
        int max = Math.max(1, appProperties.getNotice().getWebsocket().getMaxSessionsPerUser());
        while (userSessions.size() >= max) {
            Iterator<WebSocketSession> iterator = userSessions.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            WebSocketSession oldest = iterator.next();
            userSessions.remove(oldest);
            closeQuietly(oldest);
        }
        userSessions.add(session);
        log.debug("WebSocket 连接建立: userId={}, sessionId={}, 当前连接数={}",
                userId, session.getId(), userSessions.size());
    }

    /**
     * 移除会话；集合为空时删除 key，防止内存泄漏。
     */
    public void remove(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.removeIf(item -> sessionId.equals(item.getId()));
        if (userSessions.isEmpty()) {
            sessions.remove(userId, userSessions);
        }
    }

    /**
     * 某用户的全部在线会话（只读）。
     */
    public Set<WebSocketSession> getSessions(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<WebSocketSession> userSessions = sessions.get(userId);
        return userSessions == null ? Collections.emptySet() : userSessions;
    }

    private void closeQuietly(WebSocketSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close(CloseStatus.NORMAL);
        } catch (IOException e) {
            log.debug("关闭 WebSocket 会话失败: {}", e.getMessage());
        }
    }
}
