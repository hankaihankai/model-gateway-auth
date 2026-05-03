package com.model.gateway.auth.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.config.GatewayCredentialProperties;
import com.model.gateway.auth.context.LoginUser;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.dto.GatewayCredentialEnsureRequest;
import com.model.gateway.auth.exception.AuthStatusException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.support.SecretFileUtils;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * APISIX网关凭证业务服务。
 */
@Service
public class GatewayCredentialService {

    /**
     * Authorization请求头中Bearer Token前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 网关凭证配置属性。
     */
    private final GatewayCredentialProperties credentialProperties;

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
     * @param userMapper 用户数据访问对象
     * @param newApiBindingService new-api绑定业务服务
     */
    public GatewayCredentialService(
            GatewayCredentialProperties credentialProperties,
            UserMapper userMapper,
            NewApiBindingService newApiBindingService) {
        this.credentialProperties = credentialProperties;
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
        String token = extractBearerToken(authorization);
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "Token无效或已过期");
        }
        Long tokenUserId = Long.valueOf(loginId.toString());
        // Sa-Token JWT-Mixin模式下StpUtil.logout()不会清理session/terminal,只能依赖last-active key
        // 是否存在判定token是否仍然有效。logout会显式DEL该key,登出后此处必然落空。
        String lastActiveKey = SaManager.getConfig().getTokenName() + ":login:last-active:" + token;
        boolean tokenAlive = SaManager.getSaTokenDao().get(lastActiveKey) != null;
        if (!tokenAlive) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "Token已登出或已过期");
        }
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

        return newApiBindingService.ensureCredential(LoginUser.from(user));
    }

    /**
     * 从Authorization请求头提取Bearer Token。
     *
     * @param authorization Authorization请求头
     * @return Bearer Token
     */
    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "Authorization请求头缺失");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 校验APISIX回源密钥。
     *
     * @param gatewaySecret APISIX回源密钥
     */
    private void checkGatewaySecret(String gatewaySecret) {
        String configuredSecret = SecretFileUtils.readRequiredTrimmed(credentialProperties.getGatewaySecretFile(), "APISIX回源密钥");
        if (!configuredSecret.equals(gatewaySecret)) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "APISIX回源密钥错误");
        }
    }
}
