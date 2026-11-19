package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 看板单列：状态 + 计数 + 前 N 条工单。
 *
 * @author school-repair
 */
@Data
public class BoardColumnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private String statusLabel;
    private Long count;
    private List<OrderListItemVO> orders;
}
