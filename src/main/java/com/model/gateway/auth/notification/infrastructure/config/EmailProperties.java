package com.model.gateway.auth.notification.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email")
@Data
public class EmailProperties {
    private SmtpConfig qq;
    private SmtpConfig netease;
    private SmtpConfig generic;

    @Data
    public static class SmtpConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private String from;
    }
}
