package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关凭证响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayCredentialResponse {

    /**
     * 业务用户ID。
     */
    private Long userId;

    /**
     * new-api用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api用户名。
     */
    private String newApiUserName;

    /**
     * 加密后的new-api API Key。
     */
    private String apiKeyCipher;

    /**
     * 凭证状态。
     */
    private Integer status;

    /**
     * 过期时间戳秒。
     */
    private Long expireAt;

    /**
     * 更新时间戳秒。
     */
    private Long updatedAt;
}
