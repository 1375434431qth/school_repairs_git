package com.cqust.repair.module.ai.service.impl;

import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.ai.dto.AiMessage;
import com.cqust.repair.module.ai.service.ConversationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会话历史 Redis 实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiMessage> load(Long userId, String conversationId, int maxTurns) {
        String key = buildKey(userId, conversationId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            List<AiMessage> messages = objectMapper.readValue(json, new TypeReference<List<AiMessage>>() {
            });
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }
            int limit = Math.max(1, maxTurns) * 2;
            return messages.size() <= limit
                    ? messages
                    : new ArrayList<>(messages.subList(messages.size() - limit, messages.size()));
        } catch (Exception e) {
            log.warn("读取 AI 会话历史失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void append(Long userId, String conversationId, AiMessage user, AiMessage assistant,
                       int maxTurns, int ttlMinutes) {
        List<AiMessage> messages = new ArrayList<>(load(userId, conversationId, maxTurns));
        messages.add(user);
        messages.add(assistant);
        int limit = Math.max(1, maxTurns) * 2;
        if (messages.size() > limit) {
            messages = new ArrayList<>(messages.subList(messages.size() - limit, messages.size()));
        }
        try {
            stringRedisTemplate.opsForValue().set(buildKey(userId, conversationId),
                    objectMapper.writeValueAsString(messages), ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入 AI 会话历史失败: {}", e.getMessage());
        }
    }

    @Override
    public void clear(Long userId, String conversationId) {
        try {
            stringRedisTemplate.delete(buildKey(userId, conversationId));
        } catch (Exception e) {
            log.warn("清空 AI 会话历史失败: {}", e.getMessage());
        }
    }

    private String buildKey(Long userId, String conversationId) {
        return RedisKeys.AI_CHAT_HISTORY + userId + ":" + conversationId;
    }
}
