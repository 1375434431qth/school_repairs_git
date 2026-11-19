package com.cqust.repair.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新 token 请求。
 *
 * @author school-repair
 */
@Data
public class RefreshRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
