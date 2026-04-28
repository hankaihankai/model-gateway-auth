package com.model.gateway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关凭证补齐请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayCredentialEnsureRequest {

    /**
     * 业务用户ID。
     */
    private Long userId;

    /**
     * APISIX请求ID。
     */
    private String requestId;
}
