package com.cqust.repair.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新建用户请求（管理员）。
 *
 * @author school-repair
 */
@Data
public class UserCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "账号为 3~32 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6~20 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名过长")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** STUDENT/REPAIRER/MANAGER */
    @NotBlank(message = "角色不能为空")
    private String role;

    private Long buildingId;
    private Long roomId;
    /** 维修工技能标签 */
    private List<String> skillTags;
    /** 1 启用 0 禁用，缺省启用 */
    private Integer status;
    /** 管理员授权楼栋（仅 role=MANAGER 生效） */
    private List<Long> managedBuildingIds;
}
