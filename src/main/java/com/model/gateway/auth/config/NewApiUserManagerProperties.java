package com.model.gateway.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * new-api外部用户管理接口配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "new-api.user-manager")
public class NewApiUserManagerProperties {

    /**
     * new-api服务基础地址。
     */
    private String baseUrl;

    /**
     * 外部用户管理接口授权码。
     */
    private String authKey;

    /**
     * HTTP请求超时时间毫秒数。
     */
    private Integer timeoutMillis = 5000;
}
