package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.AdminUserCreateRequest;
import com.model.gateway.auth.service.UserProfileService;
import com.model.gateway.auth.vo.AdminUserCreateResponse;
import com.model.gateway.auth.vo.UserProfileVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人用户接口控制器。
 */
@RestController
public class UserController {

    /**
     * 个人用户资料业务服务。
     */
    private final UserProfileService userProfileService;

    /**
     * 创建个人用户接口控制器。
     *
     * @param userProfileService 个人用户资料业务服务
     */
    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 查询当前用户资料。
     *
     * @param authorization Authorization请求头
     * @return 当前用户资料
     */
    @GetMapping("/api/user/profile")
    public ApiResponse<UserProfileVo> profile(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success(userProfileService.getProfile(authorization));
    }

    /**
     * 注册用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @PostMapping("/api/admin/users/registerUser")
    public ApiResponse<AdminUserCreateResponse> registerUser(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(userProfileService.createUser(request));
    }
}
