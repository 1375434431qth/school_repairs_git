package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 工单数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /**
     * 状态 CAS 更新：WHERE id + status=当前状态，影响 0 行说明并发冲突。
     *
     * @param fields 需要一并更新的字段（camelCase key）
     */
    int transitUpdate(@Param("orderId") Long orderId,
                      @Param("currentStatus") String currentStatus,
                      @Param("targetStatus") String targetStatus,
                      @Param("fields") Map<String, Object> fields);
}
