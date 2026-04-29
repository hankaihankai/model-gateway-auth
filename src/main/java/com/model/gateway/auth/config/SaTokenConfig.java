package com.model.gateway.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 网关JWT路由鉴权配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 网关JWT接口鉴权拦截器。
     */
    private final GatewayJwtAuthInterceptor gatewayJwtAuthInterceptor;

    /**
     * 创建网关JWT路由鉴权配置。
     *
     * @param gatewayJwtAuthInterceptor 网关JWT接口鉴权拦截器
     */
    public SaTokenConfig(GatewayJwtAuthInterceptor gatewayJwtAuthInterceptor) {
        this.gatewayJwtAuthInterceptor = gatewayJwtAuthInterceptor;
    }

    /**
     * 注册网关JWT登录校验和管理员角色校验拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayJwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/user/registerUser",
                        "/api/gateway/new-api-credential/ensure"
                );
    }
}
