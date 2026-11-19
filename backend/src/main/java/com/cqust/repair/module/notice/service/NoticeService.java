package com.cqust.repair.module.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqust.repair.common.enums.NoticeTypeEnum;
import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.PageResult;
import com.cqust.repair.common.result.ResultCode;
import com.cqust.repair.infra.cache.RedisCacheService;
import com.cqust.repair.infra.cache.RedisKeys;
import com.cqust.repair.module.notice.dto.ManagerNoticeVO;
import com.cqust.repair.module.notice.dto.NoticeVO;
import com.cqust.repair.module.notice.entity.Notice;
import com.cqust.repair.module.notice.mapper.NoticeMapper;
import com.cqust.repair.module.user.entity.SysUser;
import com.cqust.repair.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 站内通知原子服务：落库、查询、已读、未读数缓存。
 *
 * @author school-repair
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    /** 未读数缓存 1 小时（文档 4.4 缓存表） */
    private static final long UNREAD_CACHE_SECONDS = 3600;
    private static final int MAX_PAGE_SIZE = 50;

    private final NoticeMapper noticeMapper;
    private final RedisCacheService redisCacheService;
    private final UserService userService;

    /**
     * 通知落库（REQUIRES_NEW 独立事务，失败不影响工单主事务）。
     *
     * @param userId  接收人
     * @param orderId 关联工单，可为空
     * @param type    通知类型
     * @param title   标题
     * @param content 内容
     * @return 落库后的通知
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Notice save(Long userId, Long orderId, String type, String title, String content) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setOrderId(orderId);
        notice.setType(type);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setReadFlag(0);
        noticeMapper.insert(notice);
        redisCacheService.delete(RedisKeys.NOTIFY_UNREAD + userId);
        return notice;
    }

    /**
     * 我的通知分页。
     *
     * @param readFlag 0 未读 / 1 已读 / null 全部
     */
    public PageResult<NoticeVO> page(Long userId, Integer readFlag, long pageNum, long pageSize) {
        if (readFlag != null && readFlag != 0 && readFlag != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "readFlag 取值 0 或 1");
        }
        long safePageNum = pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize < 1 ? 10L : Math.min(pageSize, MAX_PAGE_SIZE);
        QueryWrapper<Notice> wrapper = new QueryWrapper<Notice>().eq("user_id", userId);
        if (readFlag != null) {
            wrapper.eq("read_flag", readFlag);
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<Notice> page = noticeMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        List<NoticeVO> records = new ArrayList<>(page.getRecords().size());
        for (Notice notice : page.getRecords()) {
            records.add(toVO(notice));
        }
        return PageResult.of(page, records);
    }

    /**
     * 全部通知分页（管理员视角，含接收人），支持类型、已读状态、接收人关键词筛选。
     */
    public PageResult<ManagerNoticeVO> pageAll(long pageNum, long pageSize,
                                               String type, Integer readFlag, String keyword) {
        if (readFlag != null && readFlag != 0 && readFlag != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "readFlag 取值 0 或 1");
        }
        String normalizedType = null;
        if (StringUtils.hasText(type)) {
            normalizedType = type.trim();
            try {
                NoticeTypeEnum.valueOf(normalizedType);
            } catch (IllegalArgumentException e) {
                throw new BizException(ResultCode.PARAM_ERROR, "通知类型不合法");
            }
        }
        long safePageNum = pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize < 1 ? 10L : Math.min(pageSize, MAX_PAGE_SIZE);
        QueryWrapper<Notice> wrapper = new QueryWrapper<>();
        if (normalizedType != null) {
            wrapper.eq("type", normalizedType);
        }
        if (readFlag != null) {
            wrapper.eq("read_flag", readFlag);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> receiverIds = userService.listIdsByKeyword(keyword);
            if (receiverIds.isEmpty()) {
                return PageResult.empty(safePageNum, safePageSize);
            }
            wrapper.in("user_id", receiverIds);
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        Page<Notice> page = noticeMapper.selectPage(new Page<>(safePageNum, safePageSize), wrapper);
        Set<Long> userIds = new HashSet<>();
        for (Notice notice : page.getRecords()) {
            if (notice.getUserId() != null) {
                userIds.add(notice.getUserId());
            }
        }
        Map<Long, SysUser> userMap = userService.mapByIds(userIds);
        List<ManagerNoticeVO> records = new ArrayList<>(page.getRecords().size());
        for (Notice notice : page.getRecords()) {
            ManagerNoticeVO vo = new ManagerNoticeVO();
            vo.setId(notice.getId());
            vo.setOrderId(notice.getOrderId());
            vo.setReceiverId(notice.getUserId());
            SysUser receiver = userMap.get(notice.getUserId());
            vo.setReceiverName(receiver == null ? null : receiver.getRealName());
            vo.setType(notice.getType());
            vo.setTypeName(typeLabel(notice.getType()));
            vo.setTitle(notice.getTitle());
            vo.setContent(notice.getContent());
            vo.setReadFlag(notice.getReadFlag());
            vo.setCreateTime(notice.getCreateTime());
            records.add(vo);
        }
        return PageResult.of(page, records);
    }

    /**
     * 通知类型编码转中文，未知编码原样返回。
     */
    private String typeLabel(String type) {
        if (type == null) {
            return null;
        }
        try {
            return NoticeTypeEnum.valueOf(type).getLabel();
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /**
     * 标记单条通知已读（仅限本人，重复标记幂等）。
     */
    public void markRead(Long userId, Long noticeId) {
        Notice notice = noticeId == null ? null : noticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "通知不存在");
        }
        if (notice.getReadFlag() != null && notice.getReadFlag() == 1) {
            return;
        }
        noticeMapper.update(null, new UpdateWrapper<Notice>()
                .eq("id", noticeId)
                .eq("user_id", userId)
                .eq("read_flag", 0)
                .set("read_flag", 1));
        redisCacheService.delete(RedisKeys.NOTIFY_UNREAD + userId);
    }

    /**
     * 全部标记已读。
     */
    public void markAllRead(Long userId) {
        noticeMapper.update(null, new UpdateWrapper<Notice>()
                .eq("user_id", userId)
                .eq("read_flag", 0)
                .set("read_flag", 1));
        redisCacheService.delete(RedisKeys.NOTIFY_UNREAD + userId);
    }

    /**
     * 未读通知数（Redis 缓存 1 小时，写操作时失效）。
     */
    public long unreadCount(Long userId) {
        String key = RedisKeys.NOTIFY_UNREAD + userId;
        String cached = redisCacheService.getString(key);
        if (cached != null) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException ignored) {
                log.warn("未读数缓存格式异常，回源数据库: key={}", key);
            }
        }
        Long count = noticeMapper.selectCount(new QueryWrapper<Notice>()
                .eq("user_id", userId)
                .eq("read_flag", 0));
        long result = count == null ? 0L : count;
        redisCacheService.setString(key, String.valueOf(result), UNREAD_CACHE_SECONDS);
        return result;
    }

    /**
     * 实体转 VO。
     */
    public NoticeVO toVO(Notice notice) {
        NoticeVO vo = new NoticeVO();
        vo.setId(notice.getId());
        vo.setOrderId(notice.getOrderId());
        vo.setType(notice.getType());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setReadFlag(notice.getReadFlag());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }
}
