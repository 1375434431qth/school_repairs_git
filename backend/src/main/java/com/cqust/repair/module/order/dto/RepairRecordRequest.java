package com.cqust.repair.module.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保存维修过程 + 配件请求（BR-05：每次整单覆盖）。
 *
 * @author school-repair
 */
@Data
public class RepairRecordRequest {

    @NotBlank(message = "维修过程不能为空")
    @Size(min = 10, max = 1000, message = "维修过程需 10~1000 字")
    private String processDesc;

    @Valid
    private List<PartItem> parts;

    /** true 表示确认无需更换配件 */
    private Boolean noPartsFlag;

    /**
     * 配件明细项。
     */
    @Data
    public static class PartItem {

        @NotBlank(message = "配件名称不能为空")
        @Size(max = 50, message = "配件名称不能超过 50 字")
        private String partName;

        @NotNull(message = "配件数量不能为空")
        @DecimalMin(value = "0.01", message = "配件数量必须大于 0")
        @Digits(integer = 6, fraction = 2, message = "配件数量最多 6 位整数 2 位小数")
        private BigDecimal quantity;

        @Size(max = 10, message = "单位不能超过 10 字")
        private String unit;

        @DecimalMin(value = "0", message = "单价不能为负数")
        @Digits(integer = 8, fraction = 2, message = "单价最多 8 位整数 2 位小数")
        private BigDecimal unitPrice;
    }
}
