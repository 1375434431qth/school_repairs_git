package com.cqust.repair.module.order.dto;

import lombok.Data;

/**
 * 管理员工单列表查询条件。
 *
 * @author school-repair
 */
@Data
public class ManagerOrderQuery {

    private Long buildingId;
    private String status;
    private Integer priority;
    /** 0 未超时 / 1 已超时 */
    private Integer timeoutFlag;
    private Long assigneeId;
    /** 创建时间起，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private String beginTime;
    /** 创建时间止，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss（日期时含当天） */
    private String endTime;
    private String keyword;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
