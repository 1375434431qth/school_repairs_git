package com.cqust.repair.module.basedata.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 故障类型树节点。
 *
 * @author school-repair
 */
@Data
public class FaultTypeNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String name;
    private String category;
    private String categoryName;
    private Integer defaultPriority;
    private Integer expectHours;
    private Integer sort;
    /** 子节点，仅一级分类有值 */
    private List<FaultTypeNodeVO> children = new ArrayList<>();
}
