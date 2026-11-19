package com.cqust.repair.module.basedata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 房间新增 / 修改请求。
 *
 * @author school-repair
 */
@Data
public class RoomSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    @NotBlank(message = "房间号不能为空")
    @Size(max = 10, message = "房间号过长")
    private String roomNo;

    @Min(value = 1, message = "楼层至少为 1")
    @Max(value = 99, message = "楼层不能超过 99")
    private Integer floor;
}
