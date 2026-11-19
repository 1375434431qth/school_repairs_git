package com.cqust.repair.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
