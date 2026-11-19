package com.cqust.repair.module.order.biz;

import com.cqust.repair.module.order.dto.FinishRequest;
import com.cqust.repair.module.order.dto.RepairRecordRequest;

/**
 * 维修业务编排：到场登记、维修过程与配件、完工提交。
 *
 * @author school-repair
 */
public interface RepairBiz {

    /**
     * 登记到场（ACCEPTED → ARRIVED，重复提交幂等）。
     */
    void arrive(Long orderId);

    /**
     * 保存维修过程 + 配件（整单覆盖）。
     */
    void saveRecord(Long orderId, RepairRecordRequest request);

    /**
     * 提交完工（ARRIVED → FINISHED）。
     */
    void finish(Long orderId, FinishRequest request);
}
