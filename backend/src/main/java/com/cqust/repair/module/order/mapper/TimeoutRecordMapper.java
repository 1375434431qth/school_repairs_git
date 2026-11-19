package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.TimeoutRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 超时记录数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface TimeoutRecordMapper extends BaseMapper<TimeoutRecord> {
}
