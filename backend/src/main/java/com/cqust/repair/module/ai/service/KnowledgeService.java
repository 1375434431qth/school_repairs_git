package com.cqust.repair.module.ai.service;

/**
 * 知识库检索服务。
 *
 * @author school-repair
 */
public interface KnowledgeService {

    /**
     * 按问题与角色检索知识片段。
     *
     * @param question 用户问题
     * @param role     当前角色（STUDENT/REPAIRER/MANAGER）
     * @return 上下文与相关链接
     */
    KnowledgeResult search(String question, String role);
}
