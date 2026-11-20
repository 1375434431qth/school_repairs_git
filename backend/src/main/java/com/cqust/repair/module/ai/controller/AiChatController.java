package com.cqust.repair.module.ai.controller;

import com.cqust.repair.common.result.R;
import com.cqust.repair.module.ai.biz.AiChatBiz;
import com.cqust.repair.module.ai.dto.AiChatRequest;
import com.cqust.repair.module.ai.dto.AiChatVO;
import com.cqust.repair.module.ai.dto.AiStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 智能助手接口（登录用户可用）。
 *
 * @author school-repair
 */
@Tag(name = "智能助手")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatBiz aiChatBiz;

    @Operation(summary = "智能助手对话（非流式）")
    @PostMapping("/chat")
    public R<AiChatVO> chat(@Valid @RequestBody AiChatRequest request) {
        return R.success(aiChatBiz.chat(request));
    }

    @Operation(summary = "智能助手对话（SSE 流式）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request,
                                 HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return aiChatBiz.chatStream(request);
    }

    @Operation(summary = "智能助手状态")
    @GetMapping("/status")
    public R<AiStatusVO> status() {
        return R.success(aiChatBiz.status());
    }

    @Operation(summary = "清空会话")
    @DeleteMapping("/conversations/{conversationId}")
    public R<Void> clear(@PathVariable String conversationId) {
        aiChatBiz.clearConversation(conversationId);
        return R.success();
    }
}
