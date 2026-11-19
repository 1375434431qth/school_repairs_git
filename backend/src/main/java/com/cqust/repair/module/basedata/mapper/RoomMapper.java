package com.cqust.repair.module.basedata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.basedata.entity.Room;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {
}
