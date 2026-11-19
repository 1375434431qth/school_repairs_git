package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 更换配件明细（t_part_usage）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_part_usage")
public class PartUsage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 所属维修轮次 */
    private Integer roundNo;

    /** 库存件 id，非库存登记件可为空 */
    private Long partId;

    private String partName;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal unitPrice;
}
