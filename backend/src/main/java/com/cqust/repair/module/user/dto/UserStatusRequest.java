package com.cqust.repair.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号启用 / 禁用请求（管理员操作）。
 *
 * @author school-repair
 */
@Data
public class UserStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "status 不能为空")
    @Min(value = 0, message = "status 只能为 0 或 1")
    @Max(value = 1, message = "status 只能为 0 或 1")
    private Integer status;
}
