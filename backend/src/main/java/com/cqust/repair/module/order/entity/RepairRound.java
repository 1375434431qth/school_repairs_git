package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 维修轮次（t_repair_round）：同一工单每返修一次新增一轮。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_repair_round")
public class RepairRound extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    /** 轮次，从 1 开始 */
    private Integer roundNo;
    private Long repairerId;
    /** CREATED/ASSIGNED/ACCEPTED/ARRIVED/FINISHED */
    private String status;
    private String dispatchType;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime arriveTime;
    private LocalDateTime finishTime;
    private String finishDesc;
    /** 本轮进入返修的原因（roundNo > 1） */
    private String reworkReason;
    private Integer timeoutFlag;
}
