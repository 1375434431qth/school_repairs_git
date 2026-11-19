package com.cqust.repair.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqust.repair.module.order.entity.DispatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 派单记录数据访问。
 *
 * @author school-repair
 */
@Mapper
public interface DispatchRecordMapper extends BaseMapper<DispatchRecord> {

    /**
     * 查询某维修工累计拒单次数达到阈值的工单 id（BR-02：拒 2 次后不再推送给该维修工）。
     *
     * @param repairerId 维修工 id
     * @param minCount   拒绝次数阈值
     */
    List<Long> listRejectedOrderIds(@Param("repairerId") Long repairerId,
                                    @Param("minCount") int minCount);
}
