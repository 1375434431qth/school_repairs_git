package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单图片实体（t_order_image，只存 GridFS 引用）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_image")
public class OrderImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    /** 所属维修轮次 */
    private Integer roundNo;
    /** BEFORE / AFTER */
    private String imgType;
    private String fileId;
    private String fileName;
    private Long fileSize;
    private Integer sort;
}
