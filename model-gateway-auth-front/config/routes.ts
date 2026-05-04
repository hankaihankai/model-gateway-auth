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
  {
    path: '/user-manage',
    name: '用户管理',
    icon: 'UserOutlined',
    access: 'canSeeAdmin',
    routes: [
      { path: '/user-manage', redirect: '/user-manage/list' },
      { path: '/user-manage/list', name: '用户列表', component: './UserManage/List' },
      { path: '/user-manage/detail/:userId', name: '用户详情', component: './UserManage/Detail', hideInMenu: true },
    ],
  },
  { path: '*', component: './exception/404' },
];
