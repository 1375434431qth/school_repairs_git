package com.cqust.repair.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 学生反馈未修好（发起返修）请求。
 *
 * @author school-repair
 */
@Data
public class ReopenRequest {

    /** 未修好原因，必填，便于维修工定位问题 */
    @NotBlank(message = "请填写未修好的具体原因")
    @Size(max = 200, message = "原因不能超过 200 字")
    private String reason;

    /** 可选：补充问题照片（1~6 张） */
    @Size(max = 6, message = "补充照片最多 6 张")
    private List<String> imageFileIds;
}
