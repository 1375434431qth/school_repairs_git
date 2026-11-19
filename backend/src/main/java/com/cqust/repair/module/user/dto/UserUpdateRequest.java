package com.cqust.repair.module.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑用户基础信息请求（管理员）。
 *
 * @author school-repair
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 32, message = "姓名过长")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Long buildingId;
    private Long roomId;
    /** 维修工技能标签 */
    private List<String> skillTags;
}
