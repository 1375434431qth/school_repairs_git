package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单验收记录展示对象。
 *
 * @author school-repair
 */
@Data
public class AcceptanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer roundNo;
    /** PASS / REJECT */
    private String result;
    private String resultLabel;
    private String reason;
    private LocalDateTime createTime;
}
