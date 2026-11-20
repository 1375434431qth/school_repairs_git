package com.cqust.repair.module.ai.service.impl;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.module.ai.dto.AiMessage;
import com.cqust.repair.module.ai.service.AiChatService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI DeepSeek 对话实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<StreamingChatModel> streamingChatModelProvider;
    private final AppProperties appProperties;

    @PostConstruct
    public void init() {
        log.info("AI 模型装配: chat={}, streaming={}",
                chatModelProvider.getIfAvailable() != null,
                streamingChatModelProvider.getIfAvailable() != null);
    }

    @Override
    public boolean isAvailable() {
        return appProperties.getAi().isEnabled() && chatModelProvider.getIfAvailable() != null;
    }

    @Override
    public String model() {
        return appProperties.getAi().getModel();
    }

    @Override
    public String chat(String systemPrompt, List<AiMessage> history, String question) {
        if (!isAvailable()) {
            throw new BizException(ResultCode.AI_DISABLED);
        }
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        try {
            ChatResponse response = chatModel.call(buildPrompt(systemPrompt, history, question));
            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null) {
                throw new IllegalStateException("模型返回为空");
            }
            String answer = response.getResult().getOutput().getText();
            if (!StringUtils.hasText(answer)) {
                throw new IllegalStateException("模型回答为空");
            }
            return answer.trim();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 DeepSeek 失败: {}", safeError(e));
            throw new BizException(ResultCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public Flux<String> stream(String systemPrompt, List<AiMessage> history, String question) {
        if (!isAvailable()) {
            return Flux.error(new BizException(ResultCode.AI_DISABLED));
        }
        StreamingChatModel streamingChatModel = streamingChatModelProvider.getIfAvailable();
        if (streamingChatModel == null) {
            // 没有流式模型时降级为一次性返回，保证功能可用
            return Flux.defer(() -> Flux.just(chat(systemPrompt, history, question)));
        }
        try {
            Prompt prompt = buildPrompt(systemPrompt, history, question);
            return streamingChatModel.stream(prompt)
                    .map(this::extractText)
                    .filter(StringUtils::hasText);
        } catch (Exception e) {
            log.error("调用 DeepSeek 流式接口失败: {}", safeError(e));
            return Flux.error(new BizException(ResultCode.AI_SERVICE_UNAVAILABLE));
        }
    }

    private Prompt buildPrompt(String systemPrompt, List<AiMessage> history, String question) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        if (history != null) {
            for (AiMessage item : history) {
                if (item == null || !StringUtils.hasText(item.getContent())) {
                    continue;
                }
                if ("assistant".equals(item.getRole())) {
                    messages.add(new AssistantMessage(item.getContent()));
                } else {
                    messages.add(new UserMessage(item.getContent()));
                }
            }
        }
        messages.add(new UserMessage(question));
        return new Prompt(messages);
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * 提取安全的错误信息：优先保留 HTTP 状态码，否则只保留异常类型。
     */
    private String safeError(Exception e) {
        String message = e.getMessage();
        if (message != null && message.startsWith("HTTP ")) {
            int first = message.indexOf(' ');
            int second = message.indexOf(' ', first + 1);
            return second > first ? message.substring(0, second) : message;
        }
        return e.getClass().getSimpleName();
    }
}
