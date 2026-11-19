package com.cqust.repair.module.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录 / 刷新响应。
 *
 * @author school-repair
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String tokenType = "Bearer";
    /** access token 有效期（秒） */
    private Long expiresIn;
    /** 刷新令牌，用于 access token 过期后换新 */
    private String refreshToken;
    private UserInfoVO user;
}
