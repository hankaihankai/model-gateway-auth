/**
 * 前端权限策略。仅决定菜单/路由是否可见，不负责跳转登录。
 */
export default function access(initialState: { currentUser?: API.UserInfo } | undefined) {
  const permissions = initialState?.currentUser?.permissions ?? [];
  const hasPermission = (code: string) => permissions.includes(code);
  return {
    canSeeAdmin: !!initialState?.currentUser,
    canSeeRbac: hasPermission('role:view') || hasPermission('menu:view') || hasPermission('api-permission:view'),
    canSeeUserManage: hasPermission('user:view'),
    canSeeRoleManage: hasPermission('role:view'),
    canSeeMenuManage: hasPermission('menu:view'),
    canSeeApiPermissionManage: hasPermission('api-permission:view'),
    canWriteUser: hasPermission('user:write'),
    canWriteUserRole: hasPermission('user:role'),
    canWriteUserAmount: hasPermission('user:amount'),
    canWriteRole: hasPermission('role:write'),
    canWriteMenu: hasPermission('menu:write'),
    canWriteApiPermission: hasPermission('api-permission:write'),
  };
}
