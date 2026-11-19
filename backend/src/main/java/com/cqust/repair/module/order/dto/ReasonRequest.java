package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 带必填原因的操作请求（拒单 / 释放）。
 *
 * @author school-repair
 */
@Data
public class ReasonRequest {

    @NotBlank(message = "原因不能为空")
    @Size(max = 200, message = "原因不能超过 200 字")
    private String reason;
}
