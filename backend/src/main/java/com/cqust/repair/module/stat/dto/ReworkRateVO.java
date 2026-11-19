package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 返修率与返修房间 Top10。
 *
 * @author school-repair
 */
@Data
public class ReworkRateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Double rate;
    private Long reworkCount;
    private Long finishedCount;
    private List<TopRoom> topRooms;

    /**
     * 返修房间。
     */
    @Data
    public static class TopRoom implements Serializable {

        private static final long serialVersionUID = 1L;

        private String buildingName;
        private String roomNo;
        private Long reworkCount;
    }
}
