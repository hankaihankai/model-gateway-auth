package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
