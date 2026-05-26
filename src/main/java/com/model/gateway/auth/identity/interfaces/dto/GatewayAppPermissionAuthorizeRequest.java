package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关应用权限鉴权请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayAppPermissionAuthorizeRequest {

    /**
     * 业务用户ID。
     */
    private Long userId;

    /**
     * 应用编码。
     */
    private String appCode;

    /**
     * HTTP方法。
     */
    private String method;

    /**
     * 应用内部路径。
     */
    private String path;

    /**
     * APISIX请求ID。
     */
    private String requestId;
}
