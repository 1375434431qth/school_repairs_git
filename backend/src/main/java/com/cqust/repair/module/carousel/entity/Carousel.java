package com.cqust.repair.module.carousel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 首页轮播图。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_carousel")
public class Carousel extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图片标题（可空） */
    private String title;
    /** GridFS ObjectId（可空，静态图时为空） */
    private String imageFileId;
    /** 图片访问地址 */
    private String imageUrl;
    /** 展示顺序，升序 */
    private Integer sort;
    /** 1 启用 0 停用 */
    private Integer status;
}
