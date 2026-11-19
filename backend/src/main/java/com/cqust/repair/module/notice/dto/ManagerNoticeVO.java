package com.cqust.repair.module.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员通知列表展示对象（含接收人）。
 *
 * @author school-repair
 */
@Data
public class ManagerNoticeVO {

    private Long id;
    private Long orderId;
    private Long receiverId;
    private String receiverName;
    /** 通知类型，见 NoticeTypeEnum */
    private String type;
    private String typeName;
    private String title;
    private String content;
    /** 0 未读 1 已读 */
    private Integer readFlag;
    private LocalDateTime createTime;
}
