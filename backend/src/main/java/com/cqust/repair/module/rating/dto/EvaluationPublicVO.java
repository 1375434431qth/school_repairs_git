package com.cqust.repair.module.rating.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价公示条目：仅暴露评分与内容，不返回学生姓名（保证匿名）。
 *
 * @author school-repair
 */
@Data
public class EvaluationPublicVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long repairerId;
    private String repairerName;
    /** 1~5 星 */
    private Integer score;
    /** 逗号分隔标签 */
    private String tags;
    private String content;
    private LocalDateTime createTime;
}
