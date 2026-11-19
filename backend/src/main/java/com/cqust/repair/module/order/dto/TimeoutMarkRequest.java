package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员手动标记超时请求。
 *
 * @author school-repair
 */
@Data
public class TimeoutMarkRequest {

    /** ACCEPT / ARRIVE / FINISH */
    @NotBlank(message = "超时环节不能为空")
    private String timeoutType;

    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;
}
