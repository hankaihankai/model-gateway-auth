package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.model.gateway.auth.system.interfaces.vo.PermissionContextVo;

/**
 * 登录响应数据。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /**
     * 访问Token。
     */
    private String accessToken;

    /**
     * Token类型。
     */
    private String tokenType;

    /**
     * Token剩余有效期秒数。
     */
    private Long expiresIn;

    /**
     * 登录用户信息。
     */
    private UserInfoVo userInfo;

    /**
     * 权限上下文。
     */
    private PermissionContextVo permissionContext;
}
