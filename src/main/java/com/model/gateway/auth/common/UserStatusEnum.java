package com.model.gateway.auth.common;

/**
 * 用户状态枚举。
 */
public enum UserStatusEnum {

    /**
     * 启用状态。
     */
    ENABLE("ENABLE", "启用"),

    /**
     * 禁用状态。
     */
    DISABLE("DISABLE", "禁用"),

    /**
     * 异常状态。
     */
    ERROR("ERROR", "异常");

    /**
     * 状态编码。
     */
    private final String code;

    /**
     * 状态说明。
     */
    private final String description;

    /**
     * 创建用户状态枚举。
     *
     * @param code 状态编码
     * @param description 状态说明
     */
    UserStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态说明。
     *
     * @return 状态说明
     */
    public String getDescription() {
        return description;
    }
}
