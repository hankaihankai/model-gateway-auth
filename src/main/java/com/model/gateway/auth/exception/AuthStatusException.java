package com.model.gateway.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 带HTTP状态码的认证业务异常。
 */
public class AuthStatusException extends RuntimeException {

    /**
     * HTTP状态码。
     */
    private final HttpStatus status;

    /**
     * 业务响应码。
     */
    private final Integer code;

    /**
     * 创建带HTTP状态码的认证业务异常。
     *
     * @param status HTTP状态码
     * @param code 业务响应码
     * @param message 异常消息
     */
    public AuthStatusException(HttpStatus status, Integer code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 获取HTTP状态码。
     *
     * @return HTTP状态码
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 获取业务响应码。
     *
     * @return 业务响应码
     */
    public Integer getCode() {
        return code;
    }
}
