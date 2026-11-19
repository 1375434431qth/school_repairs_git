package com.cqust.repair.module.basedata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 楼栋新增 / 修改请求。
 *
 * @author school-repair
 */
@Data
public class BuildingSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "楼栋名称不能为空")
    @Size(max = 50, message = "楼栋名称过长")
    private String name;

    @Size(max = 20, message = "楼栋编号过长")
    private String code;

    @Size(max = 50, message = "所属区域过长")
    private String area;

    @Min(value = 1, message = "楼层数至少为 1")
    @Max(value = 99, message = "楼层数不能超过 99")
    private Integer floors;

    @Size(max = 200, message = "备注过长")
    private String remark;
}
