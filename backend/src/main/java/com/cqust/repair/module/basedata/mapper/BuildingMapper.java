package com.cqust.repair.module.basedata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.basedata.entity.Building;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宿舍楼数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
}
