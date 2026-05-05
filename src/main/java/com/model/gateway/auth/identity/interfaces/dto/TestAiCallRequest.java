package com.model.gateway.auth.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员测试AI调用请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestAiCallRequest {

    /**
     * 用户消息内容。
     */
    private String content;
}
