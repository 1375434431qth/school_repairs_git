package com.cqust.repair.common.util;

import java.io.Serializable;

/**
 * 当前登录用户快照，由 JwtAuthenticationFilter 写入 ThreadLocal。
 *
 * @param userId   用户 id
 * @param username 登录账号
 * @param role     角色（STUDENT/REPAIRER/MANAGER）
 * @author school-repair
 */
public record CurrentUser(Long userId, String username, String role) implements Serializable {
}
