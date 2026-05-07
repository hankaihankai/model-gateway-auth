package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员重置用户密码请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminPasswordResetRequest {

    /**
     * 新明文密码。
     */
    private String newPassword;
}
