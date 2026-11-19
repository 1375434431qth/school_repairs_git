package com.cqust.repair.common.exception;

import com.cqust.repair.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，message 会原样返回给前端。
 *
 * @author school-repair
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务码 */
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
