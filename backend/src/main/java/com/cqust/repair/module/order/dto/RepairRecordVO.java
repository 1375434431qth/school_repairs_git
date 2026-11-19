package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 维修过程记录展示对象。
 *
 * @author school-repair
 */
@Data
public class RepairRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long repairerId;
    private String repairerName;
    private String processDesc;
    private String resultDesc;
    private Integer noPartsFlag;
    private Integer costMinutes;
    private LocalDateTime updateTime;
    private List<PartUsageVO> parts;

    /**
     * 配件明细。
     */
    @Data
    public static class PartUsageVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String partName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
    }
}
