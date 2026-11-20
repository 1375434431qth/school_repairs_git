package com.cqust.repair.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能助手对话请求。
 *
 * @author school-repair
 */
@Data
public class AiChatRequest {

    /** 会话标识；为空时由后端生成 UUID */
    private String conversationId;

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题内容过长")
    private String message;

    /** 当前页面路由，仅用于上下文提示 */
    @Size(max = 200, message = "页面地址过长")
    private String page;

    /** 当前工单号，一期仅做格式校验，不注入 Prompt */
    @Size(max = 32, message = "工单号过长")
    private String orderNo;
}
