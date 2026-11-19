package com.cqust.repair.module.notice.controller;

import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.R;
import com.cqust.repair.module.notice.dto.ManagerNoticeVO;
import com.cqust.repair.module.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员通知查询：查看全部用户的站内通知。
 *
 * @author school-repair
 */
@Tag(name = "管理员端-全部通知")
@RestController
@RequestMapping("/api/v1/manager/notices")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;

    @Operation(summary = "全部通知分页")
    @GetMapping
    public R<PageResult<ManagerNoticeVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Integer readFlag,
                                               @RequestParam(required = false) String keyword) {
        return R.success(noticeService.pageAll(pageNum, pageSize, type, readFlag, keyword));
    }
}
