package com.cqust.repair.module.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人资料修改请求（当前登录用户修改自己的手机号 / 头像）。
 *
 * @author school-repair
 */
@Data
public class ProfileUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 200, message = "头像地址过长")
    private String avatar;
}
