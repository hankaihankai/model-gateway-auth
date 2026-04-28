package com.model.gateway.auth.controller;

import com.model.gateway.auth.common.ApiResponse;
import com.model.gateway.auth.dto.GatewayCredentialEnsureRequest;
import com.model.gateway.auth.service.GatewayCredentialService;
import com.model.gateway.auth.vo.GatewayCredentialResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * APISIX网关凭证接口控制器。
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayCredentialController {

    /**
     * APISIX网关凭证业务服务。
     */
    private final GatewayCredentialService gatewayCredentialService;

    /**
     * 创建APISIX网关凭证接口控制器。
     *
     * @param gatewayCredentialService APISIX网关凭证业务服务
     */
    public GatewayCredentialController(GatewayCredentialService gatewayCredentialService) {
        this.gatewayCredentialService = gatewayCredentialService;
    }

    /**
     * 补齐APISIX网关凭证。
     *
     * @param gatewaySecret APISIX回源密钥
     * @param authorization Authorization请求头
     * @param request 凭证补齐请求
     * @return 加密后的网关凭证
     */
    @PostMapping("/new-api-credential/ensure")
    public ApiResponse<GatewayCredentialResponse> ensureCredential(
            @RequestHeader("X-Gateway-Secret") String gatewaySecret,
            @RequestHeader("Authorization") String authorization,
            @RequestBody GatewayCredentialEnsureRequest request) {
        return ApiResponse.success(gatewayCredentialService.ensureCredential(gatewaySecret, authorization, request));
    }
}
