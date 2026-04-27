package com.model.gateway.auth.exception;

/**
 * 认证业务异常。
 */
public class AuthException extends RuntimeException {

    /**
     * 创建认证业务异常。
     *
     * @param message 异常消息
     */
    public AuthException(String message) {
        super(message);
    }
}
