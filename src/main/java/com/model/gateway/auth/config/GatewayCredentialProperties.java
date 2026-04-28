package com.model.gateway.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关凭证配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.credential")
public class GatewayCredentialProperties {

    /**
     * AES密钥标识。
     */
    private String keyId;

    /**
     * Base64编码的32字节AES密钥。
     */
    private String aesKey;

    /**
     * APISIX回源密钥。
     */
    private String gatewaySecret;
}
