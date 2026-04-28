package com.model.gateway.auth.service;

import com.model.gateway.auth.config.GatewayJwtProperties;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 网关JWT签发和校验服务。
 */
@Service
public class GatewayJwtService {

    /**
     * 用户名Claim名称。
     */
    private static final String USERNAME_CLAIM = "username";

    /**
     * 角色Claim名称。
     */
    private static final String ROLE_CLAIM = "role";

    /**
     * 网关JWT配置属性。
     */
    private final GatewayJwtProperties jwtProperties;

    /**
     * 创建网关JWT服务。
     *
     * @param jwtProperties 网关JWT配置属性
     */
    public GatewayJwtService(GatewayJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 签发业务网关JWT。
     *
     * @param user 用户信息
     * @return JWT字符串
     */
    public String createToken(SysUser user) {
        PrivateKey privateKey = parsePrivateKey(jwtProperties.getPrivateKey());
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(getExpireSeconds());
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .issuer(jwtProperties.getIssuer())
                .audience()
                .add(jwtProperties.getAudience())
                .and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim(USERNAME_CLAIM, user.getUsername())
                .claim(ROLE_CLAIM, user.getRole())
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 校验并解析业务网关JWT。
     *
     * @param token JWT字符串
     * @return JWT Claims
     */
    public Claims parseToken(String token) {
        try {
            PublicKey publicKey = parsePublicKey(jwtProperties.getPublicKey());
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception exception) {
            throw new AuthException("Token无效或已过期");
        }
    }

    /**
     * 获取JWT有效期秒数。
     *
     * @return JWT有效期秒数
     */
    public Long getExpireSeconds() {
        if (jwtProperties.getExpireSeconds() == null || jwtProperties.getExpireSeconds() <= 0) {
            return 7200L;
        }
        return jwtProperties.getExpireSeconds();
    }

    /**
     * 从请求头提取Bearer Token。
     *
     * @param authorization Authorization请求头
     * @return JWT字符串
     */
    public String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || authorization.length() <= 7) {
            throw new AuthException("缺少Bearer Token");
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new AuthException("缺少Bearer Token");
        }
        return authorization.substring(7);
    }

    /**
     * 解析PKCS8私钥。
     *
     * @param keyText 私钥文本
     * @return 私钥对象
     */
    private PrivateKey parsePrivateKey(String keyText) {
        if (!StringUtils.hasText(keyText)) {
            throw new AuthException("JWT私钥未配置");
        }
        try {
            byte[] keyBytes = decodeKeyBytes(keyText, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new AuthException("JWT私钥配置无效");
        }
    }

    /**
     * 解析X509公钥。
     *
     * @param keyText 公钥文本
     * @return 公钥对象
     */
    private PublicKey parsePublicKey(String keyText) {
        if (!StringUtils.hasText(keyText)) {
            throw new AuthException("JWT公钥未配置");
        }
        try {
            byte[] keyBytes = decodeKeyBytes(keyText, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception exception) {
            throw new AuthException("JWT公钥配置无效");
        }
    }

    /**
     * 解码PEM或Base64密钥内容。
     *
     * @param keyText 密钥文本
     * @param keyType 密钥类型
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
