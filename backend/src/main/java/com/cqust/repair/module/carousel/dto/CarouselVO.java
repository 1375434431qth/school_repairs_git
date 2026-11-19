package com.cqust.repair.module.carousel.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 首页轮播展示对象。
 *
 * @author school-repair
 */
@Data
public class CarouselVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String imageFileId;
    private String imageUrl;
    private Integer sort;
    /** 1 启用 0 停用 */
    private Integer status;
}
