package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.PartUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配件使用明细数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface PartUsageMapper extends BaseMapper<PartUsage> {
}
