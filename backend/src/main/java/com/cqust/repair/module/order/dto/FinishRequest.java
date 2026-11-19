package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交完工请求（BR-06：维修过程已保存 + 至少 1 张完工照片）。
 *
 * @author school-repair
 */
@Data
public class FinishRequest {

    @NotBlank(message = "完工说明不能为空")
    @Size(max = 500, message = "完工说明不能超过 500 字")
    private String resultDesc;

    @NotEmpty(message = "请至少上传 1 张完工照片")
    @Size(max = 6, message = "完工照片最多 6 张")
    private List<String> afterImageFileIds;
}
