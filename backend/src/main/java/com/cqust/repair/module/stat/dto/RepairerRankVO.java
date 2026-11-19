package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修工绩效排行。
 *
 * @author school-repair
 */
@Data
public class RepairerRankVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long repairerId;
    private String repairerName;
    private Long finishCount;
    private Long avgMinutes;
    private Double avgScore;
}
