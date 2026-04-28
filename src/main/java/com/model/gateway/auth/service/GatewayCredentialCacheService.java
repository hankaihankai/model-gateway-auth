package com.model.gateway.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 网关凭证Redis缓存服务。
 */
@Service
public class GatewayCredentialCacheService {

    /**
     * Redis凭证Key前缀。
     */
    private static final String CREDENTIAL_KEY_PREFIX = "gateway:newapi:credential:";

    /**
     * Redis字符串模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * JSON对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建网关凭证Redis缓存服务。
     *
     * @param stringRedisTemplate Redis字符串模板
     * @param objectMapper JSON对象映射器
     */
    public GatewayCredentialCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 写入网关凭证缓存。
     *
     * @param response 网关凭证响应
     * @param ttlSeconds 缓存有效期秒数
     */
    public void cacheCredential(GatewayCredentialResponse response, Long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildCredentialKey(response.getUserId()),
                    objectMapper.writeValueAsString(response),
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (JsonProcessingException exception) {
            throw new AuthException("网关凭证缓存序列化失败");
        }
    }

    /**
     * 删除网关凭证缓存。
     *
     * @param userId 业务用户ID
     */
    public void deleteCredential(Long userId) {
        stringRedisTemplate.delete(buildCredentialKey(userId));
    }

    /**
     * 构建网关凭证缓存Key。
     *
     * @param userId 业务用户ID
     * @return Redis Key
     */
    private String buildCredentialKey(Long userId) {
        return CREDENTIAL_KEY_PREFIX + userId;
    }
}
