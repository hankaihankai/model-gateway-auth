package com.model.gateway.auth.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.dev33.satoken.jwt.StpLogicJwtForMixin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.context.LoginUser;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.exception.AuthStatusException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.common.UserRoleEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token路由鉴权与JWT集成配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * SaSession中缓存登录上下文用户的属性Key。
     */
    public static final String SESSION_LOGIN_USER_KEY = "loginUser";

    /**
     * 自定义RS256 JWT模板,启动时静态注入到Sa-Token。
     */
    private final RsaSaJwtTemplate rsaSaJwtTemplate;

    /**
     * 用户数据访问对象,SaInterceptor兜底查询SysUser时使用。
     */
    private final UserMapper userMapper;

    /**
     * 创建Sa-Token配置。
     *
     * @param rsaSaJwtTemplate 自定义RS256 JWT模板
     * @param userMapper 用户数据访问对象
     */
    public SaTokenConfig(RsaSaJwtTemplate rsaSaJwtTemplate, UserMapper userMapper) {
        this.rsaSaJwtTemplate = rsaSaJwtTemplate;
        this.userMapper = userMapper;
    }

    /**
     * 启动时将自定义JWT模板注入Sa-Token静态门面。
     */
    @PostConstruct
    public void registerSaJwtTemplate() {
        SaJwtUtil.setSaJwtTemplate(rsaSaJwtTemplate);
    }

    /**
     * 注册Mixin模式的StpLogic,启用JWT自包含+Redis Session混合鉴权。
     *
     * @return Mixin模式StpLogic
     */
    @Bean
    public StpLogic stpLogicJwt() {
        return new StpLogicJwtForMixin();
    }

    /**
     * 注册Sa-Token拦截器,在每个请求中执行登录态校验、Session兜底与管理员角色校验。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            ensureLoginUserInSession();
            String uri = SaHolder.getRequest().getRequestPath();
            if (uri.startsWith("/api/admin/")) {
                StpUtil.checkRole(UserRoleEnum.ADMIN.getCode());
            }
        }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/user/registerUser",
                        "/api/gateway/new-api-credential/ensure"
                );
    }

    /**
     * 兜底将LoginUser写入SaSession。Session过期重建或服务重启后首次访问时按需查DB,
     * 同时执行用户存在性与状态校验,等价替换原GatewayJwtAuthInterceptor的状态校验逻辑。
     */
    private void ensureLoginUserInSession() {
        SaSession session = StpUtil.getSession();
        if (session.get(SESSION_LOGIN_USER_KEY) != null) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthStatusException(HttpStatus.FORBIDDEN, 403, "用户已禁用");
        }
        session.set(SESSION_LOGIN_USER_KEY, LoginUser.from(user));
    }
}
