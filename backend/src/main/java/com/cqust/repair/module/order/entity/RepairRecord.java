package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 维修过程记录（t_repair_record，一单一条，重复提交覆盖）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_repair_record")
public class RepairRecord extends VersionedEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 所属维修轮次 */
    private Integer roundNo;

    private Long repairerId;

    private String processDesc;

    private String resultDesc;

    /** 1 表示确认无需更换配件（BR-06 完工前置校验） */
    private Integer noPartsFlag;

    /** 维修耗时 = finish_time - arrive_time（分钟） */
    private Integer costMinutes;
}
