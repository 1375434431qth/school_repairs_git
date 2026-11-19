package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 派单记录（t_dispatch_record）：派单 / 抢单 / 拒单 / 释放 / 调优先级留痕。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dispatch_record")
public class DispatchRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 所属维修轮次 */
    private Integer roundNo;

    /** 操作人：ASSIGN/CHANGE_PRIORITY/RELEASE=管理员，GRAB/REJECT=维修工 */
    private Long operatorId;

    private Long assigneeId;

    /** ASSIGN/GRAB/REJECT/RELEASE/CHANGE_PRIORITY */
    private String dispatchType;

    private Integer beforePriority;

    private Integer afterPriority;

    private String remark;
}
