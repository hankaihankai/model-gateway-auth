package com.model.gateway.auth.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.model.gateway.auth.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理认证业务异常。
     *
     * @param exception 认证业务异常
     * @return 失败响应
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleAuthException(AuthException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    /**
     * 处理带HTTP状态码的认证业务异常。
     *
     * @param exception 带HTTP状态码的认证业务异常
     * @return 失败响应
     */
    @ExceptionHandler(AuthStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthStatusException(AuthStatusException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * 处理未登录异常。
     *
     * @param exception 未登录异常
     * @return 未登录响应
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleNotLoginException(NotLoginException exception) {
        return ApiResponse.fail(401, "Token无效或已过期");
    }

    /**
     * 处理角色不足异常。
     *
     * @param exception 角色不足异常
     * @return 无权限响应
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleNotRoleException(NotRoleException exception) {
        return ApiResponse.fail(403, "无管理员权限");
    }
}
