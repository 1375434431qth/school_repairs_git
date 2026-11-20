package com.cqust.repair.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能助手非流式对话响应。
 *
 * @author school-repair
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO {

    /** 会话标识 */
    private String conversationId;

    /** 回答内容 */
    private String answer;

    /** 实际使用的模型 */
    private String model;

    /** 相关页面链接 */
    private List<AiLinkVO> links;
}
