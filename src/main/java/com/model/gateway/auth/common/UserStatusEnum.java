package com.model.gateway.auth.common;

/**
 * 用户状态枚举。
 */
public enum UserStatusEnum {

    /**
     * 启用状态。
     */
    ENABLE(0, "启用"),

    /**
     * 禁用状态。
     */
    DISABLE(1, "禁用"),

    /**
     * 处理中状态。
     */
    PENDING(2, "处理中"),

    /**
     * 异常状态。
     */
    ERROR(3, "异常");

    /**
     * 状态编码。
     */
    private final Integer code;

    /**
     * 状态说明。
     */
    private final String desc;

    /**
     * 创建用户状态枚举。
     *
     * @param code 状态编码
     * @param desc 状态说明
     */
    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取状态编码。
     *
     * @return 状态编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态说明。
     *
     * @return 状态说明
     */
    public String getDesc() {
        return desc;
    }
}
