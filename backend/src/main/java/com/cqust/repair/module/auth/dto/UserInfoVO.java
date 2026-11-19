package com.cqust.repair.module.auth.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前用户信息（登录 / me 接口共用）。
 *
 * @author school-repair
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String role;
    private Long buildingId;
    private String buildingName;
    private Long roomId;
    private String roomNo;
    private String avatar;
    private List<String> skillTags;
}
