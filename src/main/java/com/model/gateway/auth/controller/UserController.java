package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.UserCreateRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.service.UserProfileService;
import com.model.gateway.auth.vo.UserCreateResponse;
import com.model.gateway.auth.vo.UserProfileVo;
import com.model.gateway.auth.vo.UserTokenRecordsVo;
import org.springframework.web.bind.annotation.GetMapping;
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
 * 个人用户接口控制器。
 */
@RestController
@RequestMapping("/api/user")
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
     * 查询当前用户资料。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 当前用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVo> profile() {
        return ApiResponse.success(userProfileService.getProfile());
    }

    /**
     * 注册用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @PostMapping("/registerUser")
    public ApiResponse<UserCreateResponse> registerUser(@RequestBody UserCreateRequest request) {
        return ApiResponse.success(userProfileService.createUser(request));
    }

    /**
     * 查询当前用户Token使用记录。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param startTime 开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime 结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    @GetMapping("/token-records")
    public ApiResponse<UserTokenRecordsVo> tokenRecords(
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
        return ApiResponse.success(userProfileService.getTokenRecords(
                pageNo,
                pageSize,
                startTimestamp,
                endTimestamp,
                modelName
        ));
    }

    /**
     * 解析时间字符串为 Unix 时间戳秒。
     *
     * @param timeStr 时间字符串，格式 yyyy-MM-dd HH:mm:ss
     * @return Unix 时间戳秒，为空时返回 null
     */
    private Long parseTimestamp(String timeStr) {
        if (!org.springframework.util.StringUtils.hasText(timeStr)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (DateTimeParseException exception) {
            throw new AuthException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 查询当前用户可用模型。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 可用模型列表
     */
    @GetMapping("/models")
    public ApiResponse<List<String>> models() {
        return ApiResponse.success(userProfileService.getModels());
    }
}
