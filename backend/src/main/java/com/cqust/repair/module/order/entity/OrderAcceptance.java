package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单验收记录（t_order_acceptance）：每轮一条，PASS/REJECT。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_acceptance")
public class OrderAcceptance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    /** 被验收的轮次 */
    private Integer roundNo;
    private Long studentId;
    /** PASS / REJECT */
    private String result;
    /** 驳回原因，REJECT 必填 */
    private String reason;
}
