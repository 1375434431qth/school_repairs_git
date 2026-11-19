package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 维修轮次展示对象。
 *
 * @author school-repair
 */
@Data
public class RepairRoundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer roundNo;
    private Long repairerId;
    private String repairerName;
    private String status;
    private String statusLabel;
    private String dispatchType;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime arriveTime;
    private LocalDateTime finishTime;
    private String finishDesc;
    private String reworkReason;
    private Integer timeoutFlag;
}
