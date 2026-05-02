package com.model.gateway.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.model.gateway.auth.common.AuthConstants;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.config.SaTokenConfig;
import com.model.gateway.auth.context.LoginUser;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.dto.LoginRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.service.AuthService;
import com.model.gateway.auth.service.GatewayCredentialCacheService;
import com.model.gateway.auth.service.NewApiBindingService;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.vo.LoginResponse;
import com.model.gateway.auth.vo.UserInfoVo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证业务服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * BCrypt密码编码器。
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * new-api绑定业务服务。
     */
    private final NewApiBindingService newApiBindingService;

    /**
     * 网关凭证缓存服务。
     */
    private final GatewayCredentialCacheService gatewayCredentialCacheService;

    /**
     * 创建认证业务服务实现。
     *
     * @param userMapper 用户数据访问对象
     * @param passwordEncoder BCrypt密码编码器
     * @param newApiBindingService new-api绑定业务服务
     * @param gatewayCredentialCacheService 网关凭证缓存服务
     */
    public AuthServiceImpl(
            UserMapper userMapper,
            BCryptPasswordEncoder passwordEncoder,
            NewApiBindingService newApiBindingService,
            GatewayCredentialCacheService gatewayCredentialCacheService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.newApiBindingService = newApiBindingService;
        this.gatewayCredentialCacheService = gatewayCredentialCacheService;
    }

    /**
     * 执行用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        checkLoginRequest(request);
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("用户名或密码错误");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }

        StpUtil.login(user.getUserId());
        LoginUser loginUser = LoginUser.from(user);
        StpUtil.getSession().set(SaTokenConfig.SESSION_LOGIN_USER_KEY, loginUser);
        newApiBindingService.ensureCredential(loginUser);
        return buildLoginResponse(loginUser);
    }

    /**
     * 刷新登录Token并延长new-api凭证缓存。
     *
     * @return 登录响应
     */
    @Override
    public LoginResponse refresh() {
        LoginUser loginUser = (LoginUser) StpUtil.getSession().get(SaTokenConfig.SESSION_LOGIN_USER_KEY);
        newApiBindingService.ensureCredential(loginUser);
        return buildLoginResponse(loginUser);
    }

    /**
     * 执行用户登出。
     */
    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        gatewayCredentialCacheService.deleteCredential(userId);
        StpUtil.logout();
    }

    /**
     * 校验登录请求参数。
     *
     * @param request 登录请求
     */
    private void checkLoginRequest(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new AuthException("用户名和密码不能为空");
        }
    }

    /**
     * 构建登录响应。
     *
     * @param user 登录上下文用户
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(LoginUser user) {
        return LoginResponse.builder()
                .accessToken(StpUtil.getTokenValue())
                .tokenType(AuthConstants.TOKEN_TYPE_BEARER)
                .expiresIn(StpUtil.getTokenTimeout())
                .userInfo(UserInfoVo.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
