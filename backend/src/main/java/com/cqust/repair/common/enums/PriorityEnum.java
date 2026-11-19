package com.cqust.repair.common.enums;

import lombok.Getter;

/**
 * 工单优先级枚举：P1 紧急 / P2 高 / P3 普通 / P4 低。
 *
 * @author school-repair
 */
@Getter
public enum PriorityEnum {

    P1(1, "紧急", 4),
    P2(2, "高", 8),
    P3(3, "普通", 24),
    P4(4, "低", 72);

    /** 优先级值（1~4） */
    private final Integer value;
    /** 中文名称 */
    private final String label;
    /** 默认期望完成时长（小时） */
    private final Integer defaultHours;

    PriorityEnum(Integer value, String label, Integer defaultHours) {
        this.value = value;
        this.label = label;
        this.defaultHours = defaultHours;
    }

    /**
     * 按值解析，非法值返回 null。
     */
    public static PriorityEnum of(Integer value) {
        if (value == null) {
            return null;
        }
        for (PriorityEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
