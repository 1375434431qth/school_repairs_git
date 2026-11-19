package com.cqust.repair.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 超时记录（t_timeout_record），uk_order_type 保证同一环节只记一条。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_timeout_record")
public class TimeoutRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 所属维修轮次 */
    private Integer roundNo;

    /** ACCEPT / ARRIVE / FINISH */
    private String timeoutType;

    private LocalDateTime planTime;

    private LocalDateTime actualTime;

    private Integer overMinutes;

    /** AUTO 自动 / MANUAL 手动 */
    private String markType;

    private Long markBy;

    private LocalDateTime markTime;
}
