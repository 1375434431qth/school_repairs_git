package com.cqust.repair.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Locale;

/**
 * 通知 WebSocket 处理器：连接注册、心跳应答、断开清理。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHandler extends TextWebSocketHandler {

    /** 握手阶段写入的用户 id 属性名 */
    public static final String ATTR_USER_ID = "userId";

    private static final int SEND_TIME_LIMIT_MILLIS = 5000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;
    private static final String PONG = "{\"type\":\"pong\"}";

    private final WsSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get(ATTR_USER_ID);
        if (!(userId instanceof Long id)) {
            closeQuietly(session);
            return;
        }
        // WebSocketSession 非线程安全，用装饰器串行化发送
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT);
        sessionManager.register(id, safeSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // 心跳：兼容纯文本 ping 与 {"type":"ping"}
        if (payload != null && payload.toLowerCase(Locale.ROOT).contains("ping")) {
            session.sendMessage(new TextMessage(PONG));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WebSocket 传输异常: sessionId={}, error={}", session.getId(), exception.getMessage());
        removeSession(session);
        closeQuietly(session);
    }

    private void removeSession(WebSocketSession session) {
        Object userId = session.getAttributes().get(ATTR_USER_ID);
        if (userId instanceof Long id) {
            sessionManager.remove(id, session.getId());
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NORMAL);
        } catch (Exception ignored) {
            // 关闭失败无需处理
        }
    }
}
