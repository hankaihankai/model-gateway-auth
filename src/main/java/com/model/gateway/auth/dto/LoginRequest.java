package com.model.gateway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求参数。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码。
     */
    private String password;
}
