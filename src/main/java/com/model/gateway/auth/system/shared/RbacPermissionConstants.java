package com.model.gateway.auth.system.shared;

/**
 * RBAC权限编码常量。
 */
public final class RbacPermissionConstants {

    /**
     * 用户管理查看权限。
     */
    public static final String USER_VIEW = "system:user:view";

    /**
     * 用户管理写入权限。
     */
    public static final String USER_WRITE = "system:user:write";

    /**
     * 用户额度管理权限。
     */
    public static final String USER_AMOUNT = "system:user:amount";

    /**
     * 用户角色分配权限。
     */
    public static final String USER_ROLE = "system:user:role";

    /**
     * 角色查看权限。
     */
    public static final String ROLE_VIEW = "system:role:view";

    /**
     * 角色写入权限。
     */
    public static final String ROLE_WRITE = "system:role:write";

    /**
     * 菜单查看权限。
     */
    public static final String MENU_VIEW = "system:menu:view";

    /**
     * 菜单写入权限。
     */
    public static final String MENU_WRITE = "system:menu:write";

    /**
     * 应用查看权限。
     */
    public static final String APP_VIEW = "system:app:view";

    /**
     * 应用写入权限。
     */
    public static final String APP_WRITE = "system:app:write";

    /**
     * API权限查看权限。
     */
    public static final String API_PERMISSION_VIEW = "system:api-permission:view";

    /**
     * API权限写入权限。
     */
    public static final String API_PERMISSION_WRITE = "system:api-permission:write";

    /**
     * 创建RBAC权限编码常量。
     */
    private RbacPermissionConstants() {
    }
}
