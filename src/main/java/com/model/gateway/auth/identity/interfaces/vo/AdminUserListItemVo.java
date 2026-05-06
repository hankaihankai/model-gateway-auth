package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员用户列表项。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserListItemVo {

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
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色编码列表。
     */
    private List<String> roles;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * 是否已绑定 new-api。
     */
    private Boolean newApiBound;
}
