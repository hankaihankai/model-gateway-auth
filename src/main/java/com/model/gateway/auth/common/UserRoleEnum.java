package com.model.gateway.auth.common;

/**
 * 用户角色枚举。
 */
public enum UserRoleEnum {

    /**
     * 管理员角色。
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 普通用户角色。
     */
    USER("USER", "普通用户");

    /**
     * 角色编码。
     */
    private final String code;

    /**
     * 角色说明。
     */
    private final String desc;

    /**
     * 创建用户角色枚举。
     *
     * @param code 角色编码
     * @param desc 角色说明
     */
    UserRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取角色编码。
     *
     * @return 角色编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取角色说明。
     *
     * @return 角色说明
     */
    public String getDesc() {
        return desc;
    }
}
