package com.cqust.repair.module.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知展示对象（同时作为 WebSocket 推送报文）。
 *
 * @author school-repair
 */
@Data
public class NoticeVO {

    private Long id;
    private Long orderId;
    /** 通知类型，见 NoticeTypeEnum */
    private String type;
    private String title;
    private String content;
    /** 0 未读 1 已读 */
    private Integer readFlag;
    private LocalDateTime createTime;
}
