package com.cqust.repair.module.basedata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 故障类型新增 / 修改请求。
 *
 * @author school-repair
 */
@Data
public class FaultTypeSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0 表示一级分类 */
    private Long parentId;

    @NotBlank(message = "故障类型名称不能为空")
    @Size(max = 30, message = "名称过长")
    private String name;

    /** WATER_ELEC/DOOR_WINDOW/NETWORK/FURNITURE/OTHER */
    @NotBlank(message = "故障分类不能为空")
    private String category;

    @Min(value = 1, message = "默认优先级为 1~4")
    @Max(value = 4, message = "默认优先级为 1~4")
    private Integer defaultPriority;

    @Min(value = 1, message = "期望处理时长至少 1 小时")
    @Max(value = 720, message = "期望处理时长不能超过 720 小时")
    private Integer expectHours;

    private Integer sort;

    /** 1 启用 0 停用，缺省启用 */
    private Integer status;
}
