package com.model.gateway.auth.service;

import com.model.gateway.auth.dto.LoginRequest;
import com.model.gateway.auth.vo.LoginResponse;

/**
 * 认证业务服务。
 */
public interface AuthService {

    /**
     * 执行用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新登录Token。
     *
     * @param authorization Authorization请求头
     * @return 登录响应
     */
    LoginResponse refresh(String authorization);

    /**
     * 执行用户登出。
     *
     * @param authorization Authorization请求头
     */
    void logout(String authorization);
}
