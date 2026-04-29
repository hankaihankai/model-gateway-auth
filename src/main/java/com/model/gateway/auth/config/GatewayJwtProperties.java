package com.model.gateway.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关JWT配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.jwt")
public class GatewayJwtProperties {

    /**
     * JWT签发方。
     */
    private String issuer;

    /**
     * JWT受众。
     */
    private String audience;

    /**
     * JWT有效期秒数。
     */
    private Long expireSeconds;

    /**
     * RS256私钥文件路径。
     */
    private String privateKeyFile;

    /**
     * RS256公钥文件路径。
     */
    private String publicKeyFile;
}
