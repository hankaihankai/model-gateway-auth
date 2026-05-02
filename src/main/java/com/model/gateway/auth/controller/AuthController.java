package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.LoginRequest;
import com.model.gateway.auth.service.AuthService;
import com.model.gateway.auth.vo.LoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 认证业务服务。
     */
    private final AuthService authService;

    /**
     * 创建认证接口控制器。
     *
     * @param authService 认证业务服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录接口。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 刷新登录Token接口。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 登录响应
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh() {
        return ApiResponse.success(authService.refresh());
    }

    /**
     * 用户登出接口。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout() {
        authService.logout();
        return ApiResponse.success(Boolean.TRUE);
    }
}
