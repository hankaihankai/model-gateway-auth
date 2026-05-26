/**
 * 前端权限策略。仅决定菜单/路由是否可见，不负责跳转登录。
 */
export default function access(initialState: { currentUser?: API.UserInfo; permissionContext?: API.PermissionContext } | undefined) {
  /**
   * 安全读取本地JSON缓存。
   */
  const readStorage = <T,>(key: string): T | undefined => {
    try {
      return JSON.parse(localStorage.getItem(key) || 'null') ?? undefined;
    } catch {
      return undefined;
    }
  };
  const storedUser = readStorage<API.UserInfo>('user_info');
  const storedPermissionContext = readStorage<API.PermissionContext>('permission_context');
  const currentUser = initialState?.currentUser ?? storedUser ?? undefined;
  const permissionContext = initialState?.permissionContext ?? storedPermissionContext ?? undefined;
  const permissions = permissionContext?.permissions ?? currentUser?.permissions ?? [];
  const hasPermission = (code: string) => permissions.includes(code);
  return {
    canSeeAdmin: !!currentUser,
    canSeeRbac: hasPermission('system:role:view') || hasPermission('system:menu:view') || hasPermission('system:api-permission:view') || hasPermission('system:app:view'),
    canSeeUserManage: hasPermission('system:user:view'),
    canSeeRoleManage: hasPermission('system:role:view'),
    canSeeMenuManage: hasPermission('system:menu:view'),
    canSeeApiPermissionManage: hasPermission('system:api-permission:view') || hasPermission('system:app:view'),
    canWriteUser: hasPermission('system:user:write'),
    canWriteUserRole: hasPermission('system:user:role'),
    canWriteUserAmount: hasPermission('system:user:amount'),
    canWriteRole: hasPermission('system:role:write'),
    canWriteMenu: hasPermission('system:menu:write'),
    canWriteApp: hasPermission('system:app:write'),
    canWriteApiPermission: hasPermission('system:api-permission:write'),
  };
}
