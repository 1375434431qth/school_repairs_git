package com.cqust.repair.module.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障类型实体（t_fault_type，两级树）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fault_type")
public class FaultType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0 表示一级分类 */
    private Long parentId;
    private String name;
    /** WATER_ELEC/DOOR_WINDOW/NETWORK/FURNITURE/OTHER */
    private String category;
    private Integer defaultPriority;
    private Integer expectHours;
    private Integer sort;
    private Integer status;
}
