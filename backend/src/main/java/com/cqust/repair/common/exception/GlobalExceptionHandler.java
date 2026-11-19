package com.cqust.repair.common.exception;

import com.cqust.repair.common.result.R;
import com.cqust.repair.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：统一转换为 {@link R}，并按错误码返回对应 HTTP 状态。
 *
 * @author school-repair
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：HTTP 状态与业务码以 ResultCode 为准。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException e) {
        ResultCode rc = e.getResultCode();
        log.warn("业务异常: code={}, message={}", rc.getCode(), e.getMessage());
        return ResponseEntity.status(rc.getHttpStatus())
                .body(R.error(rc.getCode(), e.getMessage()));
    }

    /**
     * 参数校验失败：取第一条字段校验错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.status(400).body(R.error(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 缺少请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(400)
                .body(R.error(400, "缺少参数 " + e.getParameterName()));
    }

    /**
     * 上传文件超限。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(400).body(R.error(ResultCode.FILE_TOO_LARGE));
    }

    /**
     * 请求体格式错误。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(400).body(R.error(400, "请求体格式错误"));
    }

    /**
     * 未登录 / 凭证失效。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(401).body(R.error(ResultCode.UNAUTHORIZED));
    }

    /**
     * 已登录但权限不足。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(R.error(ResultCode.FORBIDDEN));
    }

    /**
     * 兜底异常：日志打全栈，响应不暴露堆栈。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(500).body(R.error(ResultCode.SYSTEM_ERROR));
    }
}
