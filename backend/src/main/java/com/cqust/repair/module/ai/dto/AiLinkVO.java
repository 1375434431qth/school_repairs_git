package com.cqust.repair.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能助手回答下方的相关页面链接。
 *
 * @author school-repair
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiLinkVO {

    /** 链接文案 */
    private String label;

    /** 前端路由地址 */
    private String path;
}
