package com.cqust.repair.common.enums;

import lombok.Getter;

/**
 * 故障分类枚举（统计口径按工单冗余字段 fault_category 聚合）。
 *
 * @author school-repair
 */
@Getter
public enum FaultCategoryEnum {

    WATER_ELEC("水电类"),
    DOOR_WINDOW("门窗类"),
    NETWORK("网络类"),
    FURNITURE("家具类"),
    OTHER("其他");

    private final String label;

    FaultCategoryEnum(String label) {
        this.label = label;
    }

    /**
     * 按编码取中文名称，未知编码原样返回。
     */
    public static String labelOf(String code) {
        for (FaultCategoryEnum item : values()) {
            if (item.name().equals(code)) {
                return item.label;
            }
        }
        return code;
    }
}
