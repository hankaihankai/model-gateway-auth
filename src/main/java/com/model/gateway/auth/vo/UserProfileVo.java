package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户资料响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileVo {

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
}
