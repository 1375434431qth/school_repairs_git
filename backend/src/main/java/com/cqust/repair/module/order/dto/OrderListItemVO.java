package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单列表项（学生 / 维修工 / 管理员列表共用）。
 *
 * @author school-repair
 */
@Data
public class OrderListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long buildingId;
    private String buildingName;
    private String roomNo;
    private String faultTypeName;
    private Integer priority;
    private String status;
    private String statusLabel;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime createTime;
    private LocalDateTime deadlineTime;
    private Integer timeoutFlag;
    private Integer isRework;
    /** 当前维修轮次 */
    private Integer currentRound;
}
