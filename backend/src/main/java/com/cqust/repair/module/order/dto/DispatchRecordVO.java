package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 派单记录展示对象。
 *
 * @author school-repair
 */
@Data
public class DispatchRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String dispatchType;
    private Long operatorId;
    private String operatorName;
    private Long assigneeId;
    private String assigneeName;
    private Integer beforePriority;
    private Integer afterPriority;
    private String remark;
    private LocalDateTime createTime;
}
