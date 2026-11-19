package com.cqust.repair.module.basedata.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员楼栋关联维护（物理删除，避免逻辑删除占用唯一键 uk_manager_building）。
 *
 * @author school-repair
 */
@Mapper
public interface ManagerBuildingAdminMapper {

    /**
     * 物理删除某管理员的全部楼栋授权。
     */
    @Delete("DELETE FROM t_manager_building WHERE manager_id = #{managerId}")
    int deleteByManagerId(@Param("managerId") Long managerId);
}
