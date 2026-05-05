/**
 * 前端权限策略。仅决定菜单/路由是否可见，不负责跳转登录。
 */
export default function access(initialState: { currentUser?: API.UserInfo } | undefined) {
  const role = initialState?.currentUser?.role;
  return {
    canSeeAdmin: !!initialState?.currentUser, // 已登录即可见菜单
    canManageUser: role === 'ADMIN',          // 留位扩展点
  };
}
