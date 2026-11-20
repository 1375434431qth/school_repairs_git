package com.cqust.repair.infra.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 智能助手日志异步落库，失败不影响主流程。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatLogService {

    private final AiChatLogRepository aiChatLogRepository;

    @Async("taskExecutor")
    public void save(AiChatLog entry) {
        try {
            aiChatLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("AI 对话日志写入失败: {}", e.getMessage());
        }
    }
}
