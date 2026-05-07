/**
 * 前端写死菜单的真相来源。后续加菜单只在这里加节点。
 */
export default [
  {
    path: '/user',
    layout: false,
    routes: [
      { path: '/user/login', component: './user/login' },
    ],
  },
  { path: '/', redirect: '/user-manage/list' },
  { path: '/account/settings', component: './account/settings', hideInMenu: true },
  {
    path: '/user-manage',
    name: '用户管理',
    icon: 'UserOutlined',
    access: 'canSeeUserManage',
    routes: [
      { path: '/user-manage', redirect: '/user-manage/list' },
      { path: '/user-manage/list', name: '用户列表', component: './UserManage/List' },
      { path: '/user-manage/detail/:userId', name: '用户详情', component: './UserManage/Detail', hideInMenu: true },
    ],
  },
  {
    path: '/rbac',
    name: '权限管理',
    icon: 'SafetyCertificateOutlined',
    access: 'canSeeRbac',
    routes: [
      { path: '/rbac', redirect: '/rbac/roles' },
      { path: '/rbac/roles', name: '角色管理', component: './Rbac/RoleList', access: 'canSeeRoleManage' },
      { path: '/rbac/menus', name: '菜单管理', component: './Rbac/MenuList', access: 'canSeeMenuManage' },
      {
        path: '/rbac/api-permissions',
        name: 'API权限',
        component: './Rbac/ApiPermissionList',
        access: 'canSeeApiPermissionManage',
      },
    ],
  },
  { path: '*', component: './exception/404' },
];
