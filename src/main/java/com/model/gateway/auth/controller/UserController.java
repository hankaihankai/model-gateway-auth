package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.UserCreateRequest;
import com.model.gateway.auth.service.UserProfileService;
import com.model.gateway.auth.vo.UserCreateResponse;
import com.model.gateway.auth.vo.UserProfileVo;
import com.model.gateway.auth.vo.UserTokenRecordsVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * 查询当前用户资料。
     *
     * @param authorization Authorization请求头
     * @return 当前用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVo> profile(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success(userProfileService.getProfile(authorization));
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
     * 查询当前用户Token使用记录。
     *
     * @param authorization Authorization请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    @GetMapping("/token-records")
    public ApiResponse<UserTokenRecordsVo> tokenRecords(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "p", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "start_timestamp", required = false) Long startTimestamp,
            @RequestParam(value = "end_timestamp", required = false) Long endTimestamp,
            @RequestParam(value = "model_name", required = false) String modelName) {
        return ApiResponse.success(userProfileService.getTokenRecords(
                authorization,
                page,
                pageSize,
                startTimestamp,
                endTimestamp,
                modelName
        ));
    }

    /**
     * 查询当前用户可用模型。
     *
     * @param authorization Authorization请求头
     * @return 可用模型列表
     */
    @GetMapping("/models")
    public ApiResponse<List<String>> models(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.success(userProfileService.getModels(authorization));
    }
}
