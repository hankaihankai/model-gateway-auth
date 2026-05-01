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
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色。
     * @see com.model.gateway.auth.common.UserRoleEnum
     */
    private String role;

    /**
     * 用户状态。
     */
    private Integer status;
}
