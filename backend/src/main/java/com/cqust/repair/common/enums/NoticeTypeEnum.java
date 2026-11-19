package com.cqust.repair.common.enums;

import lombok.Getter;

/**
 * 站内通知类型（t_notice.type），共 9 种，见开发文档 8.5。
 *
 * @author school-repair
 */
@Getter
public enum NoticeTypeEnum {

    ORDER_ASSIGNED("派单/抢单成功"),
    ORDER_REJECTED("维修工拒单"),
    ORDER_ACCEPTED("维修工接单"),
    ORDER_ARRIVED("维修工到场"),
    ORDER_FINISHED("维修完成待确认"),
    ORDER_EVALUATED("收到评价"),
    ORDER_TIMEOUT("工单超时"),
    ORDER_URGED("管理员催单"),
    ORDER_REWORK("生成返修单");

    private final String label;

    NoticeTypeEnum(String label) {
        this.label = label;
    }
}
