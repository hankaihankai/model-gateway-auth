package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理员用户详情响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDetailVo {

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
     * 是否已绑定 new-api。
     */
    private Boolean newApiBound;

    /**
     * new-api 用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api 用户名。
     */
    private String newApiUserName;

    /**
     * new-api 绑定状态。
     */
    private Integer newApiStatus;

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
