package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.RepairRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 维修过程记录数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface RepairRecordMapper extends BaseMapper<RepairRecord> {
}
