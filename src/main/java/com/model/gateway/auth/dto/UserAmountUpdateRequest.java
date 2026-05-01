package com.model.gateway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理员修改用户金额请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAmountUpdateRequest {

    /**
     * 操作模式：add、subtract、override。
     */
    private String mode;

    /**
     * 金额元。
     */
    private BigDecimal amount;
}
