package com.cqust.repair.common.enums;

import lombok.Getter;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态枚举（8 个状态，不含已删除的 REJECTED）。
 *
 * <p>状态变更唯一入口为 {@code OrderStateMachine.transit}，禁止直接 update status。</p>
 *
 * @author school-repair
 */
@Getter
public enum OrderStatusEnum {

    CREATED("待派单"),
    ASSIGNED("待接单"),
    ACCEPTED("待上门"),
    ARRIVED("维修中"),
    FINISHED("待确认"),
    REWORKING("返修中"),
    CONFIRMED("待评价"),
    CLOSED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatusEnum(String label) {
        this.label = label;
    }

    /** 允许的状态流转关系 */
    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> ALLOWED = new EnumMap<>(OrderStatusEnum.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(ASSIGNED, ACCEPTED, CANCELLED));
        ALLOWED.put(ASSIGNED, EnumSet.of(ACCEPTED, CREATED, CANCELLED));
        ALLOWED.put(ACCEPTED, EnumSet.of(ARRIVED, CREATED));
        ALLOWED.put(ARRIVED, EnumSet.of(FINISHED));
        ALLOWED.put(FINISHED, EnumSet.of(CONFIRMED, REWORKING));
        ALLOWED.put(REWORKING, EnumSet.of(ARRIVED, CREATED, CANCELLED));
        ALLOWED.put(CONFIRMED, EnumSet.of(CLOSED));
        ALLOWED.put(CLOSED, EnumSet.of(REWORKING));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatusEnum.class));
    }

    /**
     * 是否允许流转到目标状态。
     *
     * @param target 目标状态
     * @return true 允许
     */
    public boolean canTransitTo(OrderStatusEnum target) {
        return ALLOWED.get(this).contains(target);
    }

    /**
     * 是否为终态。
     */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    /**
     * 按字符串解析状态，非法值返回 null（由调用方决定如何提示）。
     */
    public static OrderStatusEnum of(String status) {
        if (status == null) {
            return null;
        }
        for (OrderStatusEnum item : values()) {
            if (item.name().equals(status)) {
                return item;
            }
        }
        return null;
    }
}
