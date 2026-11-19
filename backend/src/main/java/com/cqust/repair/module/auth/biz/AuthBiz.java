package com.cqust.repair.module.auth.biz;

import com.cqust.repair.module.auth.dto.LoginRequest;
import com.cqust.repair.module.auth.dto.LoginVO;
import com.cqust.repair.module.auth.dto.UserInfoVO;

/**
 * 认证用例编排。
 *
 * @author school-repair
 */
public interface AuthBiz {

    /**
     * 登录：校验账号 → 签发令牌 → 组装用户信息。
     *
     * @param request 登录请求
     * @param ip      客户端 IP（仅用于日志）
     */
    LoginVO login(LoginRequest request, String ip);

    /**
     * 刷新 access token。
     */
    LoginVO refresh(String refreshToken);

    /**
     * 当前登录用户信息。
     */
    UserInfoVO me();

    /**
     * 退出登录。
     */
    void logout();
}
