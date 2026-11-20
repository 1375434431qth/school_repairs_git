package com.cqust.repair.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话历史中的单条消息。
 *
 * @author school-repair
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    /** user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 毫秒时间戳 */
    private long time;

    public static AiMessage user(String content) {
        return new AiMessage("user", content, System.currentTimeMillis());
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content, System.currentTimeMillis());
    }
}
