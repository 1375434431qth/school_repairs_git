package com.cqust.repair.module.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端用户列表查询条件。
 *
 * @author school-repair
 */
@Data
public class UserAdminQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** STUDENT/REPAIRER/MANAGER，为空不过滤 */
    private String role;
    /** 1 启用 0 禁用，为空不过滤 */
    private Integer status;
    /** 所属楼栋，为空不过滤 */
    private Long buildingId;
    /** 账号 / 姓名 / 手机号模糊匹配 */
    private String keyword;
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
