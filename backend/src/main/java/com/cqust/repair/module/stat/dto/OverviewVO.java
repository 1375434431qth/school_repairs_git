package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 核心指标卡。
 *
 * @author school-repair
 */
@Data
public class OverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalOrders;
    private Long pendingCount;
    private Long timeoutCount;
    private Long todayNewCount;
    private Long avgDurationMinutes;
    private Double reworkRate;
}
