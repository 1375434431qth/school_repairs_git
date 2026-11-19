package com.cqust.repair.infra.log;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.util.CurrentUser;
import com.cqust.repair.common.util.IpUtils;
import com.cqust.repair.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 操作日志切面：异步写 MongoDB，失败仅告警。
 *
 * @author school-repair
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final int MAX_TEXT_LENGTH = 1000;

    private final OperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    @Resource(name = "taskExecutor")
    private Executor executor;

    public OperationLogAspect(OperationLogRepository operationLogRepository, ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕通知：业务成功/失败都记录，失败不影响主流程。
     */
    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                submitLog(joinPoint, opLog, start, error);
            } catch (Exception e) {
                log.warn("操作日志装配失败: {}", e.getMessage());
            }
        }
    }

    private void submitLog(ProceedingJoinPoint joinPoint, OpLog opLog, long start, Throwable error) {
        OperationLog operationLog = new OperationLog();
        CurrentUser user = SecurityUtils.getCurrentUser();
        if (user != null) {
            operationLog.setUserId(user.userId());
            operationLog.setRole(user.role());
        }
        operationLog.setModule(opLog.module());
        operationLog.setAction(opLog.action());
        operationLog.setCostMs(System.currentTimeMillis() - start);
        operationLog.setTs(LocalDateTime.now());
        operationLog.setResultCode(error == null ? 200
                : (error instanceof BizException biz ? biz.getResultCode().getCode() : 500));

        Map<String, Object> params = new LinkedHashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse
                        || args[i] instanceof MultipartFile) {
                    continue;
                }
                putParam(params, parameterNames[i], args[i]);
            }
        }
        operationLog.setParams(params);
        operationLog.setOrderId(extractOrderId(params));
        operationLog.setIp(currentIp());

        executor.execute(() -> operationLogRepository.save(operationLog));
    }

    /**
     * 参数脱敏与截断：简单类型原样存，复杂对象存 JSON 字符串。
     */
    private void putParam(Map<String, Object> params, String name, Object value) {
        if (value == null) {
            params.put(name, null);
            return;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?>) {
            params.put(name, truncate(String.valueOf(value)));
            return;
        }
        try {
            params.put(name, truncate(objectMapper.writeValueAsString(value)));
        } catch (Exception e) {
            params.put(name, value.getClass().getSimpleName());
        }
    }

    /**
     * 从参数中推断工单 id（orderId 或 id）。
     */
    private Long extractOrderId(Map<String, Object> params) {
        Object orderId = params.get("orderId");
        if (orderId instanceof Number number) {
            return number.longValue();
        }
        Object id = params.get("id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return IpUtils.getClientIp(attributes.getRequest());
        }
        return null;
    }

    private String truncate(String text) {
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}
