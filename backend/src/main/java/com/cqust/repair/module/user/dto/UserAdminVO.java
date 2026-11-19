package com.cqust.repair.module.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户视图。
 *
 * @author school-repair
 */
@Data
public class UserAdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    /** 脱敏手机号：138****1111 */
    private String phone;
    private String role;
    private String roleLabel;
    private Long buildingId;
    private String buildingName;
    private Long roomId;
    private String roomNo;
    private String avatar;
    private List<String> skillTags;
    /** 1 启用 0 禁用 */
    private Integer status;
    /** 管理员授权楼栋（仅 role=MANAGER 有值） */
    private List<Long> managedBuildingIds;
    private LocalDateTime createTime;
}
