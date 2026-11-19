package com.cqust.repair.module.notice.biz.impl;

import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.module.notice.biz.NoticeBiz;
import com.cqust.repair.module.notice.dto.NoticeVO;
import com.cqust.repair.module.notice.entity.Notice;
import com.cqust.repair.module.notice.service.NoticeService;
import com.cqust.repair.websocket.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * 通知编排实现：先落库，再在事务提交后推送 WebSocket。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeBizImpl implements NoticeBiz {

    private final NoticeService noticeService;
    private final WsPushService wsPushService;

    @Override
    public void send(Long userId, Long orderId, NoticeTypeEnum type, String title, String content) {
        if (userId == null || type == null) {
            return;
        }
        try {
            Notice notice = noticeService.save(userId, orderId, type.name(), title, content);
            pushAfterCommit(userId, noticeService.toVO(notice));
        } catch (Exception e) {
            // 通知失败不影响工单主事务
            log.error("通知发送失败: userId={}, orderId={}, type={}", userId, orderId, type, e);
        }
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, Long orderId, NoticeTypeEnum type,
                            String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : new LinkedHashSet<>(userIds)) {
            send(userId, orderId, type, title, content);
        }
    }

    /**
     * 若当前存在事务，则注册 afterCommit 回调；否则立即推送。
     */
    private void pushAfterCommit(Long userId, NoticeVO vo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    wsPushService.push(userId, vo);
                }
            });
        } else {
            wsPushService.push(userId, vo);
        }
    }
}
