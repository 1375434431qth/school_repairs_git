package com.cqust.repair.module.ai.biz;

import com.cqust.repair.module.ai.dto.AiChatRequest;
import com.cqust.repair.module.ai.dto.AiChatVO;
import com.cqust.repair.module.ai.dto.AiStatusVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 智能助手业务编排。
 *
 * @author school-repair
 */
public interface AiChatBiz {

    /**
     * 非流式对话。
     */
    AiChatVO chat(AiChatRequest request);

    /**
     * 流式对话（SSE）。
     */
    SseEmitter chatStream(AiChatRequest request);

    /**
     * 助手状态。
     */
    AiStatusVO status();

    /**
     * 清空本人会话。
     */
    void clearConversation(String conversationId);
}
