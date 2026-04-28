package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员创建用户响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserCreateResponse {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 是否创建new-api绑定。
     */
    private Boolean newApiBound;
}
