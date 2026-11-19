package com.cqust.repair.infra.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 操作日志仓储（MongoDB）。
 *
 * @author school-repair
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OperationLogRepository {

    private final MongoTemplate mongoTemplate;

    /**
     * 写入操作日志，失败只记录 warn，绝不影响主业务。
     */
    public void save(OperationLog operationLog) {
        try {
            mongoTemplate.insert(operationLog);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {}", e.getMessage());
        }
    }
}
