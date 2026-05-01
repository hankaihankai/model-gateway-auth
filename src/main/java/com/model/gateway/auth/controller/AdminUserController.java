package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.UserAmountUpdateRequest;
import com.model.gateway.auth.service.UserProfileService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员用户接口控制器。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    /**
     * 用户资料业务服务。
     */
    private final UserProfileService userProfileService;

    /**
     * 创建管理员用户接口控制器。
     *
     * @param userProfileService 用户资料业务服务
     */
    public AdminUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 管理员设置用户金额。
     *
     * @param userId 业务用户ID
     * @param request 金额修改请求
     * @return 设置结果
     */
    @PostMapping("/{userId}/amount")
    public ApiResponse<Boolean> updateUserAmount(
            @PathVariable Long userId,
            @RequestBody UserAmountUpdateRequest request) {
        userProfileService.updateUserAmount(userId, request);
        return ApiResponse.success(Boolean.TRUE);
    }
}
