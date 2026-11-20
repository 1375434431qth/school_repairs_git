package com.cqust.repair.module.ai.biz.impl;

import com.cqust.repair.common.enums.RoleEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.config.AppProperties;
import com.cqust.repair.infra.log.AiChatLog;
import com.cqust.repair.infra.log.AiChatLogService;
import com.cqust.repair.module.ai.biz.AiChatBiz;
import com.cqust.repair.module.ai.dto.AiChatRequest;
import com.cqust.repair.module.ai.dto.AiChatVO;
import com.cqust.repair.module.ai.dto.AiLinkVO;
import com.cqust.repair.module.ai.dto.AiMessage;
import com.cqust.repair.module.ai.dto.AiStatusVO;
import com.cqust.repair.module.ai.prompt.PromptBuilder;
import com.cqust.repair.module.ai.service.AiChatService;
import com.cqust.repair.module.ai.service.AiRateLimitService;
import com.cqust.repair.module.ai.service.ConversationService;
import com.cqust.repair.module.ai.service.KnowledgeResult;
import com.cqust.repair.module.ai.service.KnowledgeService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能助手业务编排实现。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatBizImpl implements AiChatBiz {

    private static final String ROLE_COMMON = "COMMON";
    private static final int MAX_LINKS = 3;
    private static final long HEARTBEAT_SECONDS = 15;
    private static final int DEFAULT_TIMEOUT_SECONDS = 90;

    /** SSE 心跳线程，守护线程，避免阻塞应用退出 */
    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "ai-sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    private final AppProperties appProperties;
    private final AiChatService aiChatService;
    private final KnowledgeService knowledgeService;
    private final ConversationService conversationService;
    private final AiRateLimitService aiRateLimitService;
    private final PromptBuilder promptBuilder;
    private final AiChatLogService aiChatLogService;

    /** 同时进行的 AI 请求上限，防止上游并发与线程资源耗尽 */
    private Semaphore concurrencyGuard;

    @PostConstruct
    public void initConcurrencyGuard() {
        concurrencyGuard = new Semaphore(
                Math.max(1, appProperties.getAi().getMaxConcurrent()), true);
    }

    @Override
    public AiChatVO chat(AiChatRequest request) {
        PreparedChat prepared = prepare(request);
        acquireSlot();
        long start = System.currentTimeMillis();
        try {
            String answer = aiChatService.chat(prepared.getSystemPrompt(),
                    prepared.getHistory(), prepared.getQuestion());
            conversationService.append(prepared.getUserId(), prepared.getConversationId(),
                    AiMessage.user(prepared.getQuestion()), AiMessage.assistant(answer),
                    appProperties.getAi().getMaxHistoryTurns(),
                    appProperties.getAi().getHistoryTtlMinutes());
            saveLog(prepared, answer, System.currentTimeMillis() - start,
                    true, null, null);
            return new AiChatVO(prepared.getConversationId(), answer,
                    aiChatService.model(), prepared.getLinks());
        } catch (BizException e) {
            saveLog(prepared, null, System.currentTimeMillis() - start,
                    false, e.getResultCode().getCode(), e.getResultCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("智能助手处理失败: {}", e.getMessage());
            saveLog(prepared, null, System.currentTimeMillis() - start,
                    false, ResultCode.AI_SERVICE_UNAVAILABLE.getCode(), "调用失败");
            throw new BizException(ResultCode.AI_SERVICE_UNAVAILABLE);
        } finally {
            releaseSlot();
        }
    }

    @Override
    public SseEmitter chatStream(AiChatRequest request) {
        PreparedChat prepared = prepare(request);
        SseEmitter emitter = new SseEmitter(timeoutMillis());
        acquireSlot();

        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeatRef = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicBoolean slotReleased = new AtomicBoolean(false);
        Runnable releaseSlotOnce = () -> {
            if (slotReleased.compareAndSet(false, true)) {
                releaseSlot();
            }
        };
        Runnable cleanup = () -> {
            closed.set(true);
            ScheduledFuture<?> heartbeat = heartbeatRef.get();
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            Disposable subscription = subscriptionRef.get();
            if (subscription != null) {
                subscription.dispose();
            }
            releaseSlotOnce.run();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        StringBuilder answer = new StringBuilder();
        long start = System.currentTimeMillis();
        try {
            if (!send(emitter, "meta", Map.of(
                    "conversationId", prepared.getConversationId(),
                    "model", aiChatService.model()))) {
                cleanup.run();
                emitter.complete();
                return emitter;
            }
            if (prepared.getLinks() != null && !prepared.getLinks().isEmpty()) {
                send(emitter, "links", prepared.getLinks());
            }
            heartbeatRef.set(HEARTBEAT_EXECUTOR.scheduleAtFixedRate(
                    () -> sendComment(emitter), HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS));

            Disposable subscription = aiChatService.stream(prepared.getSystemPrompt(),
                            prepared.getHistory(), prepared.getQuestion())
                    .subscribe(
                            delta -> {
                                if (closed.get()) {
                                    return;
                                }
                                answer.append(delta);
                                if (!send(emitter, "delta", Map.of("content", delta))) {
                                    cleanup.run();
                                }
                            },
                            error -> {
                                if (closed.get()) {
                                    return;
                                }
                                ResultCode code = error instanceof BizException bizException
                                        ? bizException.getResultCode()
                                        : ResultCode.AI_SERVICE_UNAVAILABLE;
                                saveLog(prepared, null, System.currentTimeMillis() - start,
                                        false, code.getCode(), code.getMessage());
                                send(emitter, "error", Map.of(
                                        "code", code.getCode(),
                                        "message", code.getMessage()));
                                cleanup.run();
                                emitter.complete();
                            },
                            () -> {
                                if (closed.get()) {
                                    return;
                                }
                                String fullAnswer = answer.toString();
                                if (!StringUtils.hasText(fullAnswer)) {
                                    saveLog(prepared, null, System.currentTimeMillis() - start,
                                            false, ResultCode.AI_SERVICE_UNAVAILABLE.getCode(),
                                            ResultCode.AI_SERVICE_UNAVAILABLE.getMessage());
                                    send(emitter, "error", Map.of(
                                            "code", ResultCode.AI_SERVICE_UNAVAILABLE.getCode(),
                                            "message", ResultCode.AI_SERVICE_UNAVAILABLE.getMessage()));
                                    cleanup.run();
                                    emitter.complete();
                                    return;
                                }
                                conversationService.append(prepared.getUserId(),
                                        prepared.getConversationId(),
                                        AiMessage.user(prepared.getQuestion()),
                                        AiMessage.assistant(fullAnswer),
                                        appProperties.getAi().getMaxHistoryTurns(),
                                        appProperties.getAi().getHistoryTtlMinutes());
                                saveLog(prepared, fullAnswer, System.currentTimeMillis() - start,
                                        true, null, null);
                                send(emitter, "done", Map.of("finishReason", "stop"));
                                cleanup.run();
                                emitter.complete();
                            });
            subscriptionRef.set(subscription);
        } catch (Exception e) {
            log.error("智能助手流式处理失败: {}", e.getMessage());
            saveLog(prepared, null, System.currentTimeMillis() - start,
                    false, ResultCode.AI_SERVICE_UNAVAILABLE.getCode(), "调用失败");
            send(emitter, "error", Map.of(
                    "code", ResultCode.AI_SERVICE_UNAVAILABLE.getCode(),
                    "message", ResultCode.AI_SERVICE_UNAVAILABLE.getMessage()));
            cleanup.run();
            emitter.complete();
        }
        return emitter;
    }

    @Override
    public AiStatusVO status() {
        return new AiStatusVO(aiChatService.isAvailable(), aiChatService.model());
    }

    @Override
    public void clearConversation(String conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        conversationService.clear(userId, requireConversationId(conversationId));
    }

    /**
     * 校验与上下文准备，非流式/流式共用。
     */
    private PreparedChat prepare(AiChatRequest request) {
        AppProperties.Ai ai = appProperties.getAi();
        if (!aiChatService.isAvailable()) {
            throw new BizException(ResultCode.AI_DISABLED);
        }
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        String username = SecurityUtils.getCurrentUsername();

        String question = request.getMessage() == null ? "" : request.getMessage().trim();
        if (!StringUtils.hasText(question)) {
            throw new BizException(ResultCode.PARAM_ERROR, "问题不能为空");
        }
        if (question.length() > ai.getMaxInputChars()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "问题最多 " + ai.getMaxInputChars() + " 字");
        }
        validateOrderNo(request.getOrderNo());

        String conversationId = resolveConversationId(request.getConversationId());
        String page = sanitizePage(request.getPage(), role);
        aiRateLimitService.check(userId, ai);

        List<AiMessage> history = conversationService.load(userId, conversationId,
                ai.getMaxHistoryTurns());
        KnowledgeResult knowledge = knowledgeService.search(question, role);
        String systemPrompt = promptBuilder.build(username, roleLabel(role), page,
                knowledge.getContext());
        List<AiLinkVO> links = filterLinks(knowledge.getLinks(), role);
        return new PreparedChat(userId, username, role, conversationId, question,
                systemPrompt, history, links);
    }

    /**
     * 页面路由白名单校验：非法值置空，避免提示词注入。
     */
    private String sanitizePage(String page, String role) {
        if (!StringUtils.hasText(page)) {
            return null;
        }
        String value = page.trim();
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int fragmentIndex = value.indexOf('#');
        if (fragmentIndex >= 0) {
            value = value.substring(0, fragmentIndex);
        }
        if (!value.startsWith("/")) {
            return null;
        }
        Set<String> allowed = allowedPaths(role);
        if (allowed.contains(value)) {
            return value;
        }
        if (("STUDENT".equals(role) || "REPAIRER".equals(role))
                && value.matches("^/(student|repairer)/orders/\\d+$")) {
            String prefix = "STUDENT".equals(role) ? "/student/" : "/repairer/";
            if (value.startsWith(prefix)) {
                return value;
            }
        }
        return null;
    }

    private List<AiLinkVO> filterLinks(List<AiLinkVO> links, String role) {
        if (links == null || links.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> allowed = allowedPaths(role);
        List<AiLinkVO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AiLinkVO link : links) {
            if (link == null || !StringUtils.hasText(link.getPath())
                    || !StringUtils.hasText(link.getLabel())) {
                continue;
            }
            if (!allowed.contains(link.getPath()) || !seen.add(link.getPath())) {
                continue;
            }
            result.add(link);
            if (result.size() >= MAX_LINKS) {
                break;
            }
        }
        return result;
    }

    private Set<String> allowedPaths(String role) {
        Map<String, List<String>> whitelist = appProperties.getAi().getRouteWhitelist();
        Set<String> allowed = new LinkedHashSet<>();
        allowed.addAll(whitelist.getOrDefault(ROLE_COMMON, Collections.emptyList()));
        allowed.addAll(whitelist.getOrDefault(role, Collections.emptyList()));
        return allowed;
    }

    private String resolveConversationId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(raw.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "会话标识不合法");
        }
    }

    private String requireConversationId(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BizException(ResultCode.PARAM_ERROR, "会话标识不能为空");
        }
        try {
            return UUID.fromString(raw.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "会话标识不合法");
        }
    }

    private void validateOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return;
        }
        if (!orderNo.trim().matches("^WR\\d{14}$")) {
            throw new BizException(ResultCode.PARAM_ERROR, "工单号格式不正确");
        }
    }

    private String roleLabel(String role) {
        try {
            return RoleEnum.valueOf(role).getLabel();
        } catch (IllegalArgumentException e) {
            return "用户";
        }
    }

    private void acquireSlot() {
        if (concurrencyGuard == null) {
            return;
        }
        if (!concurrencyGuard.tryAcquire()) {
            throw new BizException(ResultCode.AI_SERVICE_UNAVAILABLE, "当前使用人数较多，请稍后再试");
        }
    }

    private void releaseSlot() {
        if (concurrencyGuard != null) {
            concurrencyGuard.release();
        }
    }

    private long timeoutMillis() {
        int seconds = appProperties.getAi().getTimeoutSeconds();
        return (seconds > 0 ? seconds : DEFAULT_TIMEOUT_SECONDS) * 1000L;
    }

    private boolean send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception e) {
            log.debug("SSE 发送失败: {}", e.getMessage());
            return false;
        }
    }

    private void sendComment(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (Exception ignored) {
            // 客户端已断开时忽略
        }
    }

    private void saveLog(PreparedChat prepared, String answer, long latencyMs,
                         boolean success, Integer errorCode, String errorMsg) {
        AiChatLog entry = new AiChatLog();
        entry.setUserId(prepared.getUserId());
        entry.setUsername(prepared.getUsername());
        entry.setRole(prepared.getRole());
        entry.setConversationId(prepared.getConversationId());
        entry.setQuestion(truncate(prepared.getQuestion(), 1000));
        entry.setAnswer(truncate(answer, 2000));
        entry.setModel(aiChatService.model());
        entry.setLatencyMs(latencyMs);
        entry.setSuccess(success);
        entry.setErrorCode(errorCode);
        entry.setErrorMsg(errorMsg);
        entry.setCreateTime(LocalDateTime.now());
        aiChatLogService.save(entry);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    @Data
    @AllArgsConstructor
    private static class PreparedChat {
        private Long userId;
        private String username;
        private String role;
        private String conversationId;
        private String question;
        private String systemPrompt;
        private List<AiMessage> history;
        private List<AiLinkVO> links;
    }
}
