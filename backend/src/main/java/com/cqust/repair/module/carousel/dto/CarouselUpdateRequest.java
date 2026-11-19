package com.cqust.repair.module.carousel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改轮播请求，字段为空表示不修改。
 *
 * @author school-repair
 */
@Data
public class CarouselUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 60, message = "标题不能超过 60 字")
    private String title;

    @Min(value = 0, message = "status 只能为 0 或 1")
    @Max(value = 1, message = "status 只能为 0 或 1")
    private Integer status;

    private Integer sort;
}
