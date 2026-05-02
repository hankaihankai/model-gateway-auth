package com.model.gateway.auth.config;

import cn.dev33.satoken.jwt.SaJwtTemplate;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.model.gateway.auth.support.SecretFileUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 基于RS256非对称密钥的Sa-Token JWT模板,覆盖默认的HS256对称签名实现。
 */
@Component
public class RsaSaJwtTemplate extends SaJwtTemplate {

    /**
     * 网关JWT配置属性。
     */
    private final GatewayJwtProperties jwtProperties;

    /**
     * RSA公私钥对,启动时一次性加载并缓存。
     */
    private final KeyPair keyPair;

    /**
     * 创建基于RS256的Sa-Token JWT模板。
     *
     * @param jwtProperties 网关JWT配置属性
     */
    public RsaSaJwtTemplate(GatewayJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.keyPair = new KeyPair(loadPublicKey(), loadPrivateKey());
    }

    /**
     * 覆盖默认HS256签名工厂,改用RS256+RSA密钥对。
     *
     * @param keySecret Sa-Token配置中的jwt-secret-key,RSA模式下被忽略
     * @return RS256签名器
     */
    @Override
    public JWTSigner createSigner(String keySecret) {
        return JWTSignerUtil.createSigner("RS256", keyPair);
    }

    /**
     * 在签发前为JWT补齐业务方所需的iss/aud声明,保留与自研版本一致的Token结构。
     *
     * @param jwt 已组装好Sa-Token内置声明的JWT对象
     * @param keySecret Sa-Token配置中的jwt-secret-key
     * @return 签名后的JWT字符串
     */
    @Override
    public String generateToken(JWT jwt, String keySecret) {
        if (StringUtils.hasText(jwtProperties.getIssuer())) {
            jwt.setPayload("iss", jwtProperties.getIssuer());
        }
        if (StringUtils.hasText(jwtProperties.getAudience())) {
            jwt.setPayload("aud", jwtProperties.getAudience());
        }
        return super.generateToken(jwt, keySecret);
    }

    /**
     * 加载RSA私钥。
     *
     * @return RSA私钥
     */
    private PrivateKey loadPrivateKey() {
        String pem = SecretFileUtils.readRequiredText(jwtProperties.getPrivateKeyFile(), "JWT私钥");
        try {
            byte[] keyBytes = decodeKeyBytes(pem, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new SaJwtException("JWT私钥配置无效");
        }
    }

    /**
     * 加载RSA公钥。
     *
     * @return RSA公钥
     */
    private PublicKey loadPublicKey() {
        String pem = SecretFileUtils.readRequiredText(jwtProperties.getPublicKeyFile(), "JWT公钥");
        try {
            byte[] keyBytes = decodeKeyBytes(pem, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new SaJwtException("JWT公钥配置无效");
        }
    }

    /**
     * 解码PEM文本中的Base64密钥内容。
     *
     * @param keyText PEM文本
     * @param keyType 密钥类型标识
     * @return DER字节
     */
    private byte[] decodeKeyBytes(String keyText, String keyType) {
        String normalized = keyText
                .replace("\\n", "\n")
                .replace("-----BEGIN " + keyType + "-----", "")
                .replace("-----END " + keyType + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
    }
}
