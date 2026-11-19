package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交报修响应。
 *
 * @author school-repair
 */
@Data
public class CreateOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private String status;
    private LocalDateTime deadlineTime;
}
