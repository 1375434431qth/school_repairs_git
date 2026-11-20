package com.cqust.repair.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能助手可用状态。
 *
 * @author school-repair
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiStatusVO {

    /** 是否可用 */
    private boolean enabled;

    /** 当前模型 */
    private String model;
}
