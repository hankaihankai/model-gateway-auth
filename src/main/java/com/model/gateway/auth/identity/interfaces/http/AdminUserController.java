package com.model.gateway.auth.identity.interfaces.http;

import com.model.gateway.auth.shared.api.ApiResponse;
import com.model.gateway.auth.shared.exception.AuthException;
import com.model.gateway.auth.identity.interfaces.dto.AdminUserCreateRequest;
import com.model.gateway.auth.identity.interfaces.dto.UserAmountUpdateRequest;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserDetailVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListPageVo;
import com.model.gateway.auth.identity.interfaces.vo.UserCreateResponse;
import com.model.gateway.auth.identity.interfaces.vo.UserTokenRecordsVo;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.model.gateway.auth.identity.application.UserProfileApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 管理员用户接口控制器。
 */
@SaCheckRole("ADMIN")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    /**
     * 用户资料业务服务。
     */
    private final UserProfileApplicationService userProfileService;

    /**
     * 创建管理员用户接口控制器。
     *
     * @param userProfileService 用户资料业务服务
     */
    public AdminUserController(UserProfileApplicationService userProfileService) {
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

    /**
     * 管理员为已有用户补绑 new-api。
     *
     * @param userId 用户ID
     * @return 绑定结果
     */
    @PostMapping("/{userId}/bind-new-api")
    public ApiResponse<Boolean> bindNewApi(@PathVariable Long userId) {
        userProfileService.adminBindNewApi(userId);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 管理员修改用户状态。
     *
     * @param userId 用户ID
     * @param status 目标状态
     * @return 修改结果
     */
    @PostMapping("/{userId}/status")
    public ApiResponse<Boolean> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status) {
        userProfileService.adminUpdateStatus(userId, status);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 管理员查询用户网关Token。
     *
     * @param userId 用户ID
     * @return 用户网关JWT Token
     */
    @GetMapping("/{userId}/gateway-token")
    public ApiResponse<String> gatewayToken(@PathVariable Long userId) {
        return ApiResponse.success(userProfileService.adminGetGatewayToken(userId));
    }

    /**
     * 管理员查询用户可用模型。
     *
     * @param userId 用户ID
     * @return 可用模型列表
     */
    @GetMapping("/{userId}/models")
    public ApiResponse<List<String>> getUserModels(@PathVariable Long userId) {
        return ApiResponse.success(userProfileService.adminGetModels(userId));
    }

    /**
     * 管理员查询用户列表。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @return 用户列表分页
     */
    @GetMapping
    public ApiResponse<AdminUserListPageVo> listUsers(
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResponse.success(userProfileService.adminListUsers(username, role, status, pageNo, pageSize));
    }

    /**
     * 管理员创建用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @PostMapping
    public ApiResponse<UserCreateResponse> createUser(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(userProfileService.adminCreateUser(request));
    }

    /**
     * 管理员查询用户详情。
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailVo> getUserDetail(@PathVariable Long userId) {
        return ApiResponse.success(userProfileService.adminGetUserDetail(userId));
    }

    /**
     * 管理员查询用户Token使用记录。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    @GetMapping("/{userId}/token-records")
    public ApiResponse<UserTokenRecordsVo> getTokenRecords(
            @PathVariable Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "modelName", required = false) String modelName) {
        Long startTimestamp = parseTimestamp(startTime);
        Long endTimestamp = parseTimestamp(endTime);
        if (startTimestamp == null) {
            startTimestamp = LocalDateTime.now().minusYears(1).atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        if (endTimestamp == null) {
            endTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        return ApiResponse.success(userProfileService.adminGetTokenRecords(
                userId, pageNo, pageSize, startTimestamp, endTimestamp, modelName));
    }

    /**
     * 解析时间字符串为 Unix 时间戳秒。
     *
     * @param timeStr 时间字符串,格式 yyyy-MM-dd HH:mm:ss
     * @return Unix 时间戳秒
     */
    private Long parseTimestamp(String timeStr) {
        if (!org.springframework.util.StringUtils.hasText(timeStr)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (DateTimeParseException exception) {
            throw new AuthException("时间格式不正确,应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
