package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价（t_evaluation，uk_order 保证一单一评）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_evaluation")
public class Evaluation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 评价时的最终轮次 */
    private Integer finalRoundNo;

    private Long studentId;

    private Long repairerId;

    /** 1~5 星 */
    private Integer score;

    /** 逗号分隔标签 */
    private String tags;

    private String content;

    private Integer anonymous;
}
