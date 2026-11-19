package com.cqust.repair.module.notice.biz;

import com.cqust.repair.common.enums.NoticeTypeEnum;

import java.util.Collection;

/**
 * 通知编排：落库（独立事务）+ 事务提交后 WebSocket 推送。
 *
 * @author school-repair
 */
public interface NoticeBiz {

    /**
     * 发送通知给单个用户；失败仅记日志，不影响调用方事务。
     */
    void send(Long userId, Long orderId, NoticeTypeEnum type, String title, String content);

    /**
     * 批量发送给多个用户（自动去重）。
     */
    void sendToUsers(Collection<Long> userIds, Long orderId, NoticeTypeEnum type,
                     String title, String content);
}
