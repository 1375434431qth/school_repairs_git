package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员调整优先级请求（BR-09）。
 *
 * @author school-repair
 */
@Data
public class PriorityChangeRequest {

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级取值 1~4")
    @Max(value = 4, message = "优先级取值 1~4")
    private Integer priority;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 200, message = "调整原因不能超过 200 字")
    private String reason;
}
