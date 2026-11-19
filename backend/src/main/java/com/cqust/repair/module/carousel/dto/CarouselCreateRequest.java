package com.cqust.repair.module.carousel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增轮播请求（图片先经 /files/upload 上传）。
 *
 * @author school-repair
 */
@Data
public class CarouselCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 60, message = "标题不能超过 60 字")
    private String title;

    @Size(max = 64, message = "fileId 过长")
    private String imageFileId;

    @NotBlank(message = "图片地址不能为空")
    @Size(max = 300, message = "图片地址过长")
    private String imageUrl;

    /** 不传则追加到末尾 */
    private Integer sort;
}
