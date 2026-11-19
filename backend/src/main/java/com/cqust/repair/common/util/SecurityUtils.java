package com.cqust.repair.common.util;

import com.cqust.repair.common.exception.BizException;
import com.cqust.repair.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录用户上下文工具类。
 *
 * <p>ThreadLocal 必须在请求结束时清理（见 JwtAuthenticationFilter），
 * 否则线程池复用会串号导致越权。</p>
 *
 * @author school-repair
 */
public final class SecurityUtils {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private SecurityUtils() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取当前用户，优先取 ThreadLocal，其次从 SecurityContext 兜底。
     *
     * @return 当前用户，未登录返回 null
     */
    public static CurrentUser getCurrentUser() {
        CurrentUser user = HOLDER.get();
        if (user != null) {
            return user;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long userId)) {
            return null;
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
        String username = authentication.getDetails() == null ? null : authentication.getDetails().toString();
        return new CurrentUser(userId, username, role);
    }

    /**
     * 获取当前用户 id，未登录直接抛 401。
     */
    public static Long getCurrentUserId() {
        CurrentUser user = getCurrentUser();
        if (user == null || user.userId() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user.userId();
    }

    /**
     * 获取当前角色，未登录直接抛 401。
     */
    public static String getCurrentRole() {
        CurrentUser user = getCurrentUser();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user.role();
    }

    /**
     * 获取当前登录账号，未登录返回 null。
     */
    public static String getCurrentUsername() {
        CurrentUser user = getCurrentUser();
        return user == null ? null : user.username();
    }
}
