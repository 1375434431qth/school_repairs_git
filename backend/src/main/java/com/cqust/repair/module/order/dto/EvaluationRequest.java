package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交评价请求（BR-08：每单只能评价一次）。
 *
 * @author school-repair
 */
@Data
public class EvaluationRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分取值 1~5")
    @Max(value = 5, message = "评分取值 1~5")
    private Integer score;

    @Size(max = 100, message = "标签不能超过 100 字")
    private String tags;

    @Size(max = 500, message = "评价内容不能超过 500 字")
    private String content;

    /** 是否匿名：1 匿名 */
    private Boolean anonymous;
}
