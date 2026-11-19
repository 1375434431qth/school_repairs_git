package com.cqust.repair.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cqust.repair.common.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体（t_user，三端统一）。
 *
 * @author school-repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，禁止序列化到响应 */
    @JsonIgnore
    private String password;

    private String realName;
    private String phone;
    /** STUDENT/REPAIRER/MANAGER */
    private String role;
    private Long buildingId;
    private Long roomId;
    private String avatar;
    /** 维修工技能标签，逗号分隔 */
    private String skillTags;
    /** 1 启用 0 禁用 */
    private Integer status;
}
