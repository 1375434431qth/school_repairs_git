package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 维修工负载（/base/repairers、/manager/repairers/load 共用）。
 *
 * @author school-repair
 */
@Data
public class RepairerLoadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long repairerId;
    private String username;
    private String realName;
    private Long buildingId;
    private String buildingName;
    private String skillTags;
    /** 在手单数（ASSIGNED/ACCEPTED/ARRIVED） */
    private Integer handCount;
    /** 今日完工数 */
    private Integer todayFinishCount;
}
