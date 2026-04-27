package com.model.gateway.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模型网关认证服务启动类。
 */
@MapperScan("com.model.gateway.auth.mapper")
@SpringBootApplication
public class ModelGatewayAuthApplication {

    /**
     * 启动模型网关认证服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ModelGatewayAuthApplication.class, args);
    }
}
