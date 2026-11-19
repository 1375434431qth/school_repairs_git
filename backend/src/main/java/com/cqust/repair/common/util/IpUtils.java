package com.cqust.repair.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 获取工具（兼容 Nginx 反向代理）。
 *
 * @author school-repair
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isValid(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValid(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip);
    }
}
