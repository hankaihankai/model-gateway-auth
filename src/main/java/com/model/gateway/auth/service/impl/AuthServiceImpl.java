package com.model.gateway.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.model.gateway.auth.common.AuthConstants;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.dto.LoginRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.service.AuthService;
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
     * 创建认证业务服务实现。
     *
     * @param userMapper 用户数据访问对象
     * @param passwordEncoder BCrypt密码编码器
     */
    public AuthServiceImpl(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
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
        if (!AuthConstants.STATUS_ENABLE.equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }

        StpUtil.login(user.getUserId());
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

    /**
     * 执行用户登出。
     */
    @Override
    public void logout() {
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
}
