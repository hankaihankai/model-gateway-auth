package com.model.gateway.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密配置。
 */
@Configuration
public class PasswordConfig {

    /**
     * 创建BCrypt密码编码器。
     *
     * @return BCrypt密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
