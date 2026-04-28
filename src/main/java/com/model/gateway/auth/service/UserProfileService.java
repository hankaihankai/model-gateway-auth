package com.model.gateway.auth.service;

import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.domain.UserNewApiBinding;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.vo.UserProfileVo;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

/**
 * 个人用户资料业务服务。
 */
@Service
public class UserProfileService {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * 网关JWT服务。
     */
    private final GatewayJwtService gatewayJwtService;

    /**
     * new-api绑定业务服务。
     */
    private final NewApiBindingService newApiBindingService;

    /**
     * 创建个人用户资料业务服务。
     *
     * @param userMapper 用户数据访问对象
     * @param gatewayJwtService 网关JWT服务
     * @param newApiBindingService new-api绑定业务服务
     */
    public UserProfileService(UserMapper userMapper, GatewayJwtService gatewayJwtService, NewApiBindingService newApiBindingService) {
        this.userMapper = userMapper;
        this.gatewayJwtService = gatewayJwtService;
        this.newApiBindingService = newApiBindingService;
    }

    /**
     * 查询当前用户资料。
     *
     * @param authorization Authorization请求头
     * @return 当前用户资料
     */
    public UserProfileVo getProfile(String authorization) {
        Claims claims = gatewayJwtService.parseToken(gatewayJwtService.extractBearerToken(authorization));
        Long userId = Long.valueOf(claims.getSubject());
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }
        UserNewApiBinding binding = newApiBindingService.getBinding(userId);
        return UserProfileVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .newApiUserId(binding.getNewApiUserId())
                .newApiUserName(binding.getNewApiUserName())
                .build();
    }
}
