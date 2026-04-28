package com.model.gateway.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.model.gateway.auth.common.AuthConstants.ROLE_ADMIN;

/**
 * Sa-Token路由鉴权配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册登录校验和管理员角色校验拦截器。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**", "/v1/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/user/profile",
                        "/api/gateway/new-api-credential/ensure"
                );

        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkRole(ROLE_ADMIN)))
                .addPathPatterns("/api/admin/**");
    }
}
