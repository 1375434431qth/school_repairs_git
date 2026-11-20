package com.cqust.repair.infra.log;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 智能助手对话日志。
 *
 * @author school-repair
 */
@Data
@Document(collection = "ai_chat_log")
public class AiChatLog {

    @Id
    private String id;

    private Long userId;

    private String username;

    private String role;

    private String conversationId;

    private String question;

    private String answer;

    private String model;

    private Long latencyMs;

    private Boolean success;

    private Integer errorCode;

    private String errorMsg;

    /** 30 天后由 MongoDB TTL 索引自动清理 */
    @Indexed(expireAfter = "30d")
    private LocalDateTime createTime;
}
