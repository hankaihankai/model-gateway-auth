package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关应用权限鉴权响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayAppPermissionAuthorizeResponse {

    /**
     * 是否允许访问。
     */
    private Boolean allowed;

    /**
     * 命中的权限编码。
     */
    private String permissionCode;
}
