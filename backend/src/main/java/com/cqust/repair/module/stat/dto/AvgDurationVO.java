package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 平均处理时长趋势。
 *
 * @author school-repair
 */
@Data
public class AvgDurationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String unit = "minute";
    private List<Point> points;

    /**
     * 单个时间点。
     */
    @Data
    public static class Point implements Serializable {

        private static final long serialVersionUID = 1L;

        private String date;
        private Long avgMinutes;
        private Long orderCount;
    }
}
