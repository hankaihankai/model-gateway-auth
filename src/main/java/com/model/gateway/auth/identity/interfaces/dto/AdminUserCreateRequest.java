package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员创建用户请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserCreateRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 明文密码。
     */
    private String password;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 是否同步绑定 new-api,默认 true。
     */
    private Boolean bindNewApi;
}
