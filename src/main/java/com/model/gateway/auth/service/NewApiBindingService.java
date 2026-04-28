package com.model.gateway.auth.service;

import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.domain.UserNewApiBinding;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.mapper.UserNewApiBindingMapper;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * new-api绑定业务服务。
 */
@Service
public class NewApiBindingService {

    /**
     * new-api绑定数据访问对象。
     */
    private final UserNewApiBindingMapper bindingMapper;

    /**
     * new-api凭证加密服务。
     */
    private final CredentialCryptoService credentialCryptoService;

    /**
     * 网关凭证缓存服务。
     */
    private final GatewayCredentialCacheService credentialCacheService;

    /**
     * 网关JWT服务。
     */
    private final GatewayJwtService gatewayJwtService;

    /**
     * 创建new-api绑定业务服务。
     *
     * @param bindingMapper new-api绑定数据访问对象
     * @param credentialCryptoService new-api凭证加密服务
     * @param credentialCacheService 网关凭证缓存服务
     * @param gatewayJwtService 网关JWT服务
     */
    public NewApiBindingService(
            UserNewApiBindingMapper bindingMapper,
            CredentialCryptoService credentialCryptoService,
            GatewayCredentialCacheService credentialCacheService,
            GatewayJwtService gatewayJwtService) {
        this.bindingMapper = bindingMapper;
        this.credentialCryptoService = credentialCryptoService;
        this.credentialCacheService = credentialCacheService;
        this.gatewayJwtService = gatewayJwtService;
    }

    /**
     * 确保用户存在可用new-api凭证并刷新Redis。
     *
     * @param user 用户信息
     * @return 网关凭证响应
     */
    public GatewayCredentialResponse ensureCredential(SysUser user) {
        checkUser(user);
        UserNewApiBinding binding = bindingMapper.selectByUserId(user.getUserId());
        checkBinding(binding);

        Long now = Instant.now().getEpochSecond();
        Long expireAt = now + gatewayJwtService.getExpireSeconds();
        String apiKeyCipher = credentialCryptoService.encryptApiKey(
                binding.getNewApiApiKey(),
                user.getUserId(),
                binding.getNewApiUserId(),
                binding.getNewApiUserName()
        );

        GatewayCredentialResponse response = GatewayCredentialResponse.builder()
                .userId(user.getUserId())
                .newApiUserId(binding.getNewApiUserId())
                .newApiUserName(binding.getNewApiUserName())
                .apiKeyCipher(apiKeyCipher)
                .status(UserStatusEnum.ENABLE.getCode())
                .expireAt(expireAt)
                .updatedAt(now)
                .build();
        credentialCacheService.cacheCredential(response, gatewayJwtService.getExpireSeconds());
        return response;
    }

    /**
     * 查询用户new-api绑定。
     *
     * @param userId 业务用户ID
     * @return new-api绑定
     */
    public UserNewApiBinding getBinding(Long userId) {
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        checkBinding(binding);
        return binding;
    }

    /**
     * 校验用户状态。
     *
     * @param user 用户信息
     */
    private void checkUser(SysUser user) {
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }
    }

    /**
     * 校验new-api绑定状态。
     *
     * @param binding new-api绑定
     */
    private void checkBinding(UserNewApiBinding binding) {
        if (binding == null) {
            throw new AuthException("用户未配置new-api绑定");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(binding.getStatus())) {
            throw new AuthException("用户new-api绑定已禁用");
        }
        if (binding.getNewApiUserId() == null || !StringUtils.hasText(binding.getNewApiUserName())) {
            throw new AuthException("用户new-api绑定不完整");
        }
        if (!StringUtils.hasText(binding.getNewApiApiKey())) {
            throw new AuthException("用户new-api API Key未配置");
        }
    }
}
