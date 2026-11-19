package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单主表实体（t_work_order）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_work_order")
public class WorkOrder extends VersionedEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 根工单 id（首单=自身，返修不新建工单） */
    private Long rootOrderId;

    private String orderNo;
    private Long studentId;
    private Long buildingId;
    private Long roomId;
    private String roomNo;
    private Long faultTypeId;
    private String faultTypeName;
    private String faultCategory;
    private String description;
    private String contactPhone;
    /** 1 紧急 2 高 3 普通 4 低 */
    private Integer priority;
    /** CREATED/ASSIGNED/ACCEPTED/ARRIVED/FINISHED/REWORKING/CONFIRMED/CLOSED/CANCELLED */
    private String status;
    /** 当前维修轮次，从 1 开始 */
    private Integer currentRound;
    private Integer expectHours;
    private LocalDateTime deadlineTime;
    private Long assigneeId;
    /** ASSIGN/GRAB */
    private String dispatchType;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime arriveTime;
    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
    private LocalDateTime closeTime;
    private LocalDateTime cancelTime;
    private Integer timeoutFlag;
    private String timeoutType;
    private Integer isRework;
    private Long sourceOrderId;
    private Integer evaluateFlag;
    private String cancelReason;
}
