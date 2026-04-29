package com.model.gateway.auth.service;

import com.model.gateway.auth.config.GatewayCredentialProperties;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.support.SecretFileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * new-api凭证加密服务。
 */
@Service
public class CredentialCryptoService {

    /**
     * AES-GCM随机向量字节数。
     */
    private static final int GCM_IV_SIZE = 12;

    /**
     * AES-GCM认证标签位数。
     */
    private static final int GCM_TAG_BITS = 128;

    /**
     * AES密钥字节数。
     */
    private static final int AES_KEY_SIZE = 32;

    /**
     * 安全随机数生成器。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 网关凭证配置属性。
     */
    private final GatewayCredentialProperties credentialProperties;

    /**
     * 创建new-api凭证加密服务。
     *
     * @param credentialProperties 网关凭证配置属性
     */
    public CredentialCryptoService(GatewayCredentialProperties credentialProperties) {
        this.credentialProperties = credentialProperties;
    }

    /**
     * 加密完整new-api API Key。
     *
     * @param apiKey 完整new-api API Key
     * @param userId 业务用户ID
     * @param newApiUserId new-api用户ID
     * @param newApiUserName new-api用户名
     * @return 加密后的凭证
     */
    public String encryptApiKey(String apiKey, Long userId, Long newApiUserId, String newApiUserName) {
        if (!StringUtils.hasText(apiKey)) {
            throw new AuthException("new-api API Key未配置");
        }
        try {
            byte[] aesKey = decodeAesKey();
            byte[] iv = new byte[GCM_IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(buildAad(userId, newApiUserId, newApiUserName));
            byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return "v1:" + credentialProperties.getKeyId() + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException("new-api API Key加密失败");
        }
    }

    /**
     * 构建AES-GCM附加认证数据。
     *
     * @param userId 业务用户ID
     * @param newApiUserId new-api用户ID
     * @param newApiUserName new-api用户名
     * @return 附加认证数据
     */
    private byte[] buildAad(Long userId, Long newApiUserId, String newApiUserName) {
        String aad = userId + ":" + newApiUserId + ":" + newApiUserName;
        return aad.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 解码AES密钥。
     *
     * @return AES密钥字节
     */
    private byte[] decodeAesKey() {
        if (!StringUtils.hasText(credentialProperties.getKeyId())) {
            throw new AuthException("AES密钥标识未配置");
        }
        String aesKeyText = SecretFileUtils.readRequiredTrimmed(credentialProperties.getAesKeyFile(), "AES密钥");
        byte[] aesKey = Base64.getDecoder().decode(aesKeyText);
        if (aesKey.length != AES_KEY_SIZE) {
            throw new AuthException("AES密钥必须是Base64编码的32字节内容");
        }
        return aesKey;
    }
}
