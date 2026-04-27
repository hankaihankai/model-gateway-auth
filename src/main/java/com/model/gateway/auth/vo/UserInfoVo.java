package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVo {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 用户角色。
     */
    private String role;
}
