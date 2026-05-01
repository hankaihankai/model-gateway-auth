package com.model.gateway.auth.config;

import com.model.gateway.auth.common.UserRoleEnum;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.exception.AuthStatusException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.service.GatewayCredentialCacheService;
import com.model.gateway.auth.service.GatewayJwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 网关JWT接口鉴权拦截器。
 */
@Component
public class GatewayJwtAuthInterceptor implements HandlerInterceptor {

    /**
     * 当前用户ID请求属性名。
     */
    public static final String CURRENT_USER_ID_ATTRIBUTE = "currentUserId";

    /**
     * 网关JWT服务。
     */
    private final GatewayJwtService gatewayJwtService;

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * 网关凭证缓存服务。
     */
    private final GatewayCredentialCacheService gatewayCredentialCacheService;

    /**
     * 创建网关JWT接口鉴权拦截器。
     *
     * @param gatewayJwtService 网关JWT服务
     * @param userMapper 用户数据访问对象
     * @param gatewayCredentialCacheService 网关凭证缓存服务
     */
    public GatewayJwtAuthInterceptor(
            GatewayJwtService gatewayJwtService,
            UserMapper userMapper,
            GatewayCredentialCacheService gatewayCredentialCacheService) {
        this.gatewayJwtService = gatewayJwtService;
        this.userMapper = userMapper;
        this.gatewayCredentialCacheService = gatewayCredentialCacheService;
    }

    /**
     * 校验请求中的网关JWT。
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param handler 处理器对象
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        SysUser user = readCurrentUser(request);
        if (isAdminPath(request) && !isAdmin(user)) {
            throw new AuthStatusException(HttpStatus.FORBIDDEN, 403, "无管理员权限");
        }
        request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, user.getUserId());
        return true;
    }

    /**
     * 读取当前JWT对应用户。
     *
     * @param request HTTP请求
     * @return 当前用户
     */
    private SysUser readCurrentUser(HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            String token = gatewayJwtService.extractBearerToken(authorization);
            Claims claims = gatewayJwtService.parseToken(token);
            if (gatewayCredentialCacheService.isJwtBlacklisted(token)) {
                throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "Token无效或已过期");
            }
            SysUser user = userMapper.selectByUserId(Long.valueOf(claims.getSubject()));
            if (user == null) {
                throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, "用户不存在");
            }
            if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
                throw new AuthStatusException(HttpStatus.FORBIDDEN, 403, "用户已禁用");
            }
            return user;
        } catch (AuthStatusException exception) {
            throw exception;
        } catch (AuthException | IllegalArgumentException exception) {
            throw new AuthStatusException(HttpStatus.UNAUTHORIZED, 401, exception.getMessage());
        }
    }

    /**
     * 判断请求是否为管理员接口。
     *
     * @param request HTTP请求
     * @return 是否管理员接口
     */
    private boolean isAdminPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/admin/");
    }

    /**
     * 判断用户是否为管理员。
     *
     * @param user 当前用户
     * @return 是否管理员
     */
    private boolean isAdmin(SysUser user) {
        return user.getRole() != null && UserRoleEnum.ADMIN.getCode().equalsIgnoreCase(user.getRole());
    }
}
