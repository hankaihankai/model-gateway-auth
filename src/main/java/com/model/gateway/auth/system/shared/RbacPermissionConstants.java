package com.model.gateway.auth.system.shared;

/**
 * RBAC权限编码常量。
 */
public final class RbacPermissionConstants {

    /**
     * 用户管理查看权限。
     */
    public static final String USER_VIEW = "user:view";

    /**
     * 用户管理写入权限。
     */
    public static final String USER_WRITE = "user:write";

    /**
     * 用户额度管理权限。
     */
    public static final String USER_AMOUNT = "user:amount";

    /**
     * 用户角色分配权限。
     */
    public static final String USER_ROLE = "user:role";

    /**
     * 角色查看权限。
     */
    public static final String ROLE_VIEW = "role:view";

    /**
     * 角色写入权限。
     */
    public static final String ROLE_WRITE = "role:write";

    /**
     * 菜单查看权限。
     */
    public static final String MENU_VIEW = "menu:view";

    /**
     * 菜单写入权限。
     */
    public static final String MENU_WRITE = "menu:write";

    /**
     * API权限查看权限。
     */
    public static final String API_PERMISSION_VIEW = "api-permission:view";

    /**
     * API权限写入权限。
     */
    public static final String API_PERMISSION_WRITE = "api-permission:write";

    /**
     * 创建RBAC权限编码常量。
     */
    private RbacPermissionConstants() {
    }
}
