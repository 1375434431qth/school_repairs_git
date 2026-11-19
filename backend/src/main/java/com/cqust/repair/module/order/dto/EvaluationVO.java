package com.cqust.repair.module.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价展示对象（匿名评价不返回学生姓名）。
 *
 * @author school-repair
 */
@Data
public class EvaluationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer score;
    private String tags;
    private String content;
    private Integer anonymous;
    private String studentName;
    private LocalDateTime createTime;
}
