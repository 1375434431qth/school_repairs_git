package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.OrderAcceptance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单验收记录数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface OrderAcceptanceMapper extends BaseMapper<OrderAcceptance> {
}
