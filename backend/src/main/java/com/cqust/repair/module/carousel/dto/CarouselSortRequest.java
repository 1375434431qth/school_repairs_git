package com.cqust.repair.module.carousel.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 轮播批量排序请求：按数组顺序重排。
 *
 * @author school-repair
 */
@Data
public class CarouselSortRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ids 不能为空")
    private List<Long> ids;
}
