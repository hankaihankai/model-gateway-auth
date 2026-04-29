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
     * Base64编码的32字节AES密钥文件路径。
     */
    private String aesKeyFile;

    /**
     * APISIX回源密钥文件路径。
     */
    private String gatewaySecretFile;
}
