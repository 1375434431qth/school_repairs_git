package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.RepairRound;
import org.apache.ibatis.annotations.Mapper;

/**
 * 维修轮次数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface RepairRoundMapper extends BaseMapper<RepairRound> {
}
