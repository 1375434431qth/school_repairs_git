package com.cqust.repair.module.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置：仅在开关开启且模型 Bean 存在时创建 ChatClient。
 *
 * @author school-repair
 */
@Configuration
@ConditionalOnProperty(prefix = "repair.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiConfig {

    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient aiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
