package com.cqust.repair.common.enums;

import lombok.Getter;

/**
 * 角色枚举。
 *
 * @author school-repair
 */
@Getter
public enum RoleEnum {

    STUDENT("学生"),
    REPAIRER("维修人员"),
    MANAGER("宿舍管理员");

    private final String label;

    RoleEnum(String label) {
        this.label = label;
    }
}
