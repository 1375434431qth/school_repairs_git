package com.cqust.repair.module.rating.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修工评分榜条目：某维修工全部评价的平均星级。
 *
 * @author school-repair
 */
@Data
public class RepairerRatingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long repairerId;
    private String repairerName;
    /** 平均星级（保留 1 位小数），无评价时为 null */
    private Double avgScore;
    /** 评价总数 */
    private Long ratingCount;
}
