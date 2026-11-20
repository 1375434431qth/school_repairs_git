package com.cqust.repair.module.ai.service;

import com.cqust.repair.module.ai.dto.AiMessage;

import java.util.List;

/**
 * AI 会话历史服务（Redis）。
 *
 * @author school-repair
 */
public interface ConversationService {

    /**
     * 读取最近的历史消息。
     */
    List<AiMessage> load(Long userId, String conversationId, int maxTurns);

    /**
     * 追加一轮问答并续期。
     */
    void append(Long userId, String conversationId, AiMessage user, AiMessage assistant,
                int maxTurns, int ttlMinutes);

    /**
     * 清空指定会话。
     */
    void clear(Long userId, String conversationId);
}
