package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 学生提交报修请求（不接收 priority，由服务端计算）。
 *
 * @author school-repair
 */
@Data
public class CreateOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "楼栋不能为空")
    private Long buildingId;

    @NotNull(message = "房间不能为空")
    private Long roomId;

    @NotNull(message = "故障类型不能为空")
    private Long faultTypeId;

    @NotBlank(message = "故障描述不能为空")
    @Size(min = 10, max = 500, message = "故障描述长度需在 10~500 字之间")
    private String description;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @NotEmpty(message = "请至少上传 1 张报修照片")
    @Size(max = 6, message = "报修照片最多 6 张")
    private List<@NotBlank(message = "图片 fileId 不能为空") String> imageFileIds;
}
