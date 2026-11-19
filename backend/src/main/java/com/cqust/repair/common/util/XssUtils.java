package com.cqust.repair.common.util;

import org.springframework.web.util.HtmlUtils;

/**
 * XSS 防护工具：入库前对用户输入做 HTML 转义。
 *
 * @author school-repair
 */
public final class XssUtils {

    private XssUtils() {
    }

    /**
     * HTML 转义，null 原样返回。
     */
    public static String escape(String text) {
        return text == null ? null : HtmlUtils.htmlEscape(text);
    }
}
