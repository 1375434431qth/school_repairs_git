package com.cqust.repair.module.ai.service;

import com.cqust.repair.module.ai.dto.AiMessage;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务：封装 Spring AI ChatModel 调用。
 *
 * @author school-repair
 */
public interface AiChatService {

    /**
     * 当前是否可用（开关开启且模型 Bean 存在）。
     */
    boolean isAvailable();

    /**
     * 当前模型名称。
     */
    String model();

    /**
     * 执行一次非流式对话。
     *
     * @param systemPrompt 系统提示词
     * @param history      历史消息
     * @param question     当前问题
     * @return 模型回答
     */
    String chat(String systemPrompt, List<AiMessage> history, String question);

    /**
     * 执行一次流式对话。
     *
     * @param systemPrompt 系统提示词
     * @param history      历史消息
     * @param question     当前问题
     * @return 增量文本流
     */
    Flux<String> stream(String systemPrompt, List<AiMessage> history, String question);
}
