package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户密码修改请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordUpdateRequest {

    /**
     * 旧明文密码。
     */
    private String oldPassword;

    /**
     * 新明文密码。
     */
    private String newPassword;
}
