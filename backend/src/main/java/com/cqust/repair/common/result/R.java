package com.cqust.repair.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 *
 * @param <T> 数据类型
 * @author school-repair
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码 */
    private Integer code;
    /** 提示信息（字段名固定为 message，与 Boot 默认一致） */
    private String message;
    /** 响应数据 */
    private T data;
    /** 服务端时间戳（毫秒） */
    private Long timestamp;

    private R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> success() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> success(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> success(String message, T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> error(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> error(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    public static <T> R<T> error(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> error(String message) {
        return new R<>(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }
}
