package com.model.gateway.auth.dto;

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
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private String status;

    /**
     * new-api用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api用户名。
     */
    private String newApiUserName;

    /**
     * 完整new-api API Key。
     */
    private String newApiApiKey;
}
