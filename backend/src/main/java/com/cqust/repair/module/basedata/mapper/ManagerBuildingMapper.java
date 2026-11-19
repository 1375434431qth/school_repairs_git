package com.cqust.repair.module.basedata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.basedata.entity.ManagerBuilding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员-楼栋关联数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface ManagerBuildingMapper extends BaseMapper<ManagerBuilding> {
}
