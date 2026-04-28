package com.model.gateway.auth.service;

import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.config.GatewayCredentialProperties;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.dto.GatewayCredentialEnsureRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.exception.AuthStatusException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * APISIX网关凭证业务服务。
 */
@Service
public class GatewayCredentialService {

    /**
     * 网关凭证配置属性。
     */
    private final GatewayCredentialProperties credentialProperties;

    /**
     * 网关JWT服务。
     */
    private final GatewayJwtService gatewayJwtService;

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * new-api绑定业务服务。
     */
    private final NewApiBindingService newApiBindingService;

    /**
     * 创建APISIX网关凭证业务服务。
     *
     * @param credentialProperties 网关凭证配置属性
     * @param gatewayJwtService 网关JWT服务
     * @param userMapper 用户数据访问对象
     * @param newApiBindingService new-api绑定业务服务
     */
    public GatewayCredentialService(
            GatewayCredentialProperties credentialProperties,
            GatewayJwtService gatewayJwtService,
            UserMapper userMapper,
            NewApiBindingService newApiBindingService) {
        this.credentialProperties = credentialProperties;
        this.gatewayJwtService = gatewayJwtService;
        this.userMapper = userMapper;
        this.newApiBindingService = newApiBindingService;
    }

    /**
     * 补齐APISIX网关凭证。
     *
     * @param gatewaySecret APISIX回源密钥
     * @param authorization Authorization请求头
     * @param request 凭证补齐请求
     * @return 加密后的网关凭证
     */
    public GatewayCredentialResponse ensureCredential(String gatewaySecret, String authorization, GatewayCredentialEnsureRequest request) {
        checkGatewaySecret(gatewaySecret);
        Claims claims = gatewayJwtService.parseToken(gatewayJwtService.extractBearerToken(authorization));
        Long tokenUserId = Long.valueOf(claims.getSubject());
        if (request == null || request.getUserId() == null || !tokenUserId.equals(request.getUserId())) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "Token用户不匹配");
        }

        SysUser user = userMapper.selectByUserId(request.getUserId());
        if (user == null) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthStatusException(HttpStatus.FORBIDDEN, 403, "用户已禁用");
        }

        return newApiBindingService.ensureCredential(user);
    }

    /**
     * 校验APISIX回源密钥。
     *
     * @param gatewaySecret APISIX回源密钥
     */
    private void checkGatewaySecret(String gatewaySecret) {
        if (!StringUtils.hasText(credentialProperties.getGatewaySecret())) {
            throw new AuthException("APISIX回源密钥未配置");
        }
        if (!credentialProperties.getGatewaySecret().equals(gatewaySecret)) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "APISIX回源密钥错误");
        }
    }
}
