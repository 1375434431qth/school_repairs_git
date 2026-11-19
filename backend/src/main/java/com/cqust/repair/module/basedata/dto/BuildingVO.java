package com.cqust.repair.module.basedata.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 楼栋展示对象。
 *
 * @author school-repair
 */
@Data
public class BuildingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String area;
    private Integer floors;
}
