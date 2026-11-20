package com.cqust.repair.module.ai.config;

import com.cqust.repair.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 启动校验：启用 AI 时必须注入密钥，且模型开关不能为 none。
 *
 * @author school-repair
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStartupValidator implements ApplicationRunner {

    private final AppProperties appProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Ai ai = appProperties.getAi();
        if (!ai.isEnabled()) {
            log.info("智能助手已关闭（repair.ai.enabled=false）");
            return;
        }
        String chatModel = environment.getProperty("spring.ai.model.chat", "deepseek");
        if ("none".equalsIgnoreCase(chatModel)) {
            throw new IllegalStateException(
                    "repair.ai.enabled=true 时不能设置 spring.ai.model.chat=none；"
                            + "如需关闭 AI，请同时设置 repair.ai.enabled=false");
        }
        String apiKey = environment.getProperty("spring.ai.deepseek.api-key");
        if (!StringUtils.hasText(apiKey) && ai.isFailFastOnMissingKey()) {
            throw new IllegalStateException(
                    "未检测到 DEEPSEEK_API_KEY，请通过启动命令注入，例如："
                            + "$env:DEEPSEEK_API_KEY=\"sk-...\"；mvn spring-boot:run");
        }
        log.info("智能助手已启用，模型={}", ai.getModel());
    }
}
