package com.model.gateway.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统用户实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysUser {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * BCrypt加密后的密码。
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
}
