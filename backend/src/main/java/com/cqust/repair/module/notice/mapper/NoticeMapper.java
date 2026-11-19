package com.cqust.repair.module.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.notice.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
