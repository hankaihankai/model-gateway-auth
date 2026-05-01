package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 当前用户资料响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileVo {

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private Integer status;

    /**
     * new-api用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api用户名。
     */
    private String newApiUserName;

    /**
     * 当前余额金额。
     */
    private BigDecimal currentBalanceAmount;

    /**
     * 已用额度金额。
     */
    private BigDecimal usedQuotaAmount;

    /**
     * 总额度金额。
     */
    private BigDecimal totalQuotaAmount;

    /**
     * 剩余原始额度。
     */
    private Long quota;

    /**
     * 已用原始额度。
     */
    private Long usedQuota;

    /**
     * 总原始额度。
     */
    private Long totalQuota;

    /**
     * 额度金额换算比例。
     */
    private Long quotaPerUnit;
}
