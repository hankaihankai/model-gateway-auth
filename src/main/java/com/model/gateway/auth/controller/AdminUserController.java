package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.AdminUserCreateRequest;
import com.model.gateway.auth.service.AdminUserService;
import com.model.gateway.auth.vo.AdminUserCreateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户管理接口控制器。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    /**
     * 管理员用户管理业务服务。
     */
    private final AdminUserService adminUserService;

    /**
     * 创建管理员用户管理接口控制器。
     *
     * @param adminUserService 管理员用户管理业务服务
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 注册用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @PostMapping("/registerUser")
    public ApiResponse<AdminUserCreateResponse> registerUser(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(adminUserService.createUser(request));
    }
}
