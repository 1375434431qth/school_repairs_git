package com.cqust.repair.infra.log;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 智能助手对话日志仓储。
 *
 * @author school-repair
 */
public interface AiChatLogRepository extends MongoRepository<AiChatLog, String> {
}
