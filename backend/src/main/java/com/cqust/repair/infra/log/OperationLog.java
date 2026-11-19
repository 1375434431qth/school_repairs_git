package com.cqust.repair.infra.log;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作审计日志文档（MongoDB operation_log）。
 *
 * @author school-repair
 */
@Data
@Document(collection = "operation_log")
@CompoundIndex(name = "idx_user_ts", def = "{'userId': 1, 'ts': -1}")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private Long userId;
    private String role;
    @Indexed
    private String module;
    private String action;
    private Long orderId;
    private Map<String, Object> params;
    private Integer resultCode;
    private Long costMs;
    private String ip;
    private LocalDateTime ts;
}
