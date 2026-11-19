package com.cqust.repair.module.notice.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.common.util.SecurityUtils;
import com.cqust.repair.module.notice.dto.NoticeVO;
import com.cqust.repair.module.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口（三端共用，只能读写自己的通知）。
 *
 * @author school-repair
 */
@Tag(name = "通知")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "我的通知")
    @GetMapping
    public R<PageResult<NoticeVO>> list(@RequestParam(required = false) Integer readFlag,
                                        @RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        return R.success(noticeService.page(SecurityUtils.getCurrentUserId(), readFlag, pageNum, pageSize));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        noticeService.markRead(SecurityUtils.getCurrentUserId(), id);
        return R.success();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public R<Void> readAll() {
        noticeService.markAllRead(SecurityUtils.getCurrentUserId());
        return R.success();
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.success(noticeService.unreadCount(SecurityUtils.getCurrentUserId()));
    }
}
