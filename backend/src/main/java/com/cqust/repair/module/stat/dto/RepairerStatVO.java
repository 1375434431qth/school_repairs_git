package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 维修工个人绩效。
 *
 * @author school-repair
 */
@Data
public class RepairerStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long repairerId;
    private String repairerName;
    private LocalDate beginDate;
    private LocalDate endDate;
    /** 区间内接单数（assign_time 落在区间） */
    private Long acceptCount;
    /** 区间内完工数（finish_time 落在区间） */
    private Long finishCount;
    /** 平均维修时长（分钟，接单→完工） */
    private Long avgMinutes;
    /** 平均评分（1~5，保留两位小数） */
    private Double avgScore;
}
