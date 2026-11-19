package com.cqust.repair.module.stat.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计查询条件（Mapper 入参）。
 *
 * @author school-repair
 */
@Data
public class StatQuery {

    /** 左闭 */
    private LocalDateTime beginTime;
    /** 右开（次日零点） */
    private LocalDateTime endTime;
    /** 单楼栋精确过滤 */
    private Long buildingId;
    /** 管理员授权楼栋范围（buildingId 为空时生效） */
    private List<Long> buildingIds;
    /** day / week / month */
    private String groupBy;
}
