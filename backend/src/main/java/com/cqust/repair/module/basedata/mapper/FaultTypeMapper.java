package com.cqust.repair.module.basedata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.basedata.entity.FaultType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障类型数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface FaultTypeMapper extends BaseMapper<FaultType> {
}
