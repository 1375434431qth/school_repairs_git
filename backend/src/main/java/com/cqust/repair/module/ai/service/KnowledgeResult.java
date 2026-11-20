package com.cqust.repair.module.ai.service;

import com.cqust.repair.module.ai.dto.AiLinkVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 知识检索结果。
 *
 * @author school-repair
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeResult {

    /** 拼接后的知识上下文 */
    private String context;

    /** 命中片段携带的页面链接 */
    private List<AiLinkVO> links;

    public static KnowledgeResult empty() {
        return new KnowledgeResult("", Collections.emptyList());
    }
}
