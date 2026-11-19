package com.cqust.repair.infra.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息体（RabbitMQ 可选链路）。
 *
 * @author school-repair
 */
@Data
public class OrderNoticeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息唯一 id，消费端据此幂等 */
    private String messageId;
    private Long orderId;
    private String orderNo;
    /** 通知类型，见文档 8.5 */
    private String type;
    /** 接收人 */
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
