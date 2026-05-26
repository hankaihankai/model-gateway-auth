package com.model.gateway.auth.websearch.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Serper搜索接口配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "websearch.serper")
public class SerperProperties {

    /**
     * Serper接口密钥。
     */
    private String apiKey;

    /**
     * Serper搜索接口地址。
     */
    private String endpoint = "https://google.serper.dev/search";

    /**
     * HTTP请求超时时间毫秒数。
     */
    private Integer timeoutMillis = 5000;
}
