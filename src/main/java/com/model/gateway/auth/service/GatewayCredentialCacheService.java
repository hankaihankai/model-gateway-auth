package com.model.gateway.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.Objects;

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
     * JWT黑名单Key前缀。
     */
    private static final String JWT_BLACKLIST_KEY_PREFIX = "gateway:jwt:blacklist:";

    /**
     * 用户额度锁Key前缀。
     */
    private static final String USER_QUOTA_LOCK_KEY_PREFIX = "gateway:user:quota:lock:";

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
     * 将JWT加入黑名单。
     *
     * @param token JWT字符串
     * @param expiration JWT过期时间
     */
    public void blacklistJwt(String token, Date expiration) {
        long ttlSeconds = calculateTtlSeconds(expiration);
        if (ttlSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                buildJwtBlacklistKey(token),
                "1",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    /**
     * 判断JWT是否已被加入黑名单。
     *
     * @param token JWT字符串
     * @return 是否已被加入黑名单
     */
    public boolean isJwtBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildJwtBlacklistKey(token)));
    }

    /**
     * 尝试获取用户额度分布式锁。
     *
     * @param userId 业务用户ID
     * @param lockValue 锁值
     * @param ttlSeconds 锁有效期秒数
     * @return 是否获取成功
     */
    public boolean tryLockUserQuota(Long userId, String lockValue, Long ttlSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                buildUserQuotaLockKey(userId),
                lockValue,
                Duration.ofSeconds(ttlSeconds)
        ));
    }

    /**
     * 释放用户额度分布式锁。
     *
     * @param userId 业务用户ID
     * @param lockValue 锁值
     */
    public void unlockUserQuota(Long userId, String lockValue) {
        String key = buildUserQuotaLockKey(userId);
        String currentValue = stringRedisTemplate.opsForValue().get(key);
        if (Objects.equals(currentValue, lockValue)) {
            stringRedisTemplate.delete(key);
        }
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

    /**
     * 构建JWT黑名单Key。
     *
     * @param token JWT字符串
     * @return Redis Key
     */
    public String buildJwtBlacklistKey(String token) {
        return JWT_BLACKLIST_KEY_PREFIX + sha256(token);
    }

    /**
     * 构建用户额度锁Key。
     *
     * @param userId 业务用户ID
     * @return Redis Key
     */
    private String buildUserQuotaLockKey(Long userId) {
        return USER_QUOTA_LOCK_KEY_PREFIX + userId;
    }

    /**
     * 计算JWT黑名单TTL秒数。
     *
     * @param expiration JWT过期时间
     * @return 剩余有效期秒数
     */
    private long calculateTtlSeconds(Date expiration) {
        if (expiration == null) {
            return 0;
        }
        return Math.max(0, expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond());
    }

    /**
     * 计算SHA-256摘要。
     *
     * @param value 原始字符串
     * @return 十六进制摘要
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AuthException("JWT黑名单Key生成失败");
        }
    }
}
