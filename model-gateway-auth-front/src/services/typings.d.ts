declare namespace API {
  /**
   * 后端统一响应包装。
   */
  interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
  }

  /**
   * 登录响应。
   */
  interface LoginResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    userInfo: UserInfo;
    permissionContext: PermissionContext;
  }

  /**
   * 当前登录用户简要信息。
   */
  interface UserInfo {
    userId: number;
    username: string;
    nickname: string;
    roles: string[];
    permissions: string[];
  }

  /**
   * 当前用户权限上下文。
   */
  interface PermissionContext {
    roles: string[];
    permissions: string[];
    menus: MenuTreeItem[];
  }

  /**
   * 动态菜单树节点。
   */
  interface MenuTreeItem {
    menuId: number;
    parentId: number;
    menuType: 'DIR' | 'MENU' | 'BUTTON' | string;
    menuName: string;
    path?: string;
    componentKey?: string;
    permissionCode?: string;
    icon?: string;
    visible: boolean;
    status: number;
    builtin: boolean;
    sort: number;
    children?: MenuTreeItem[];
  }

  /**
   * 用户列表单项。
   */
  interface UserListItem {
    userId: number;
    username: string;
    nickname: string;
    phone: string;
    email: string;
    roles: string[];
    status: 0 | 1 | 2 | 3;
    newApiBound: boolean;
  }

  /**
   * 用户列表分页响应。
   */
  interface UserListPageData {
    list: UserListItem[];
    total: number;
    pageNo: number;
    pageSize: number;
  }

  /**
   * 用户列表查询参数。
   */
  interface UserListQuery {
    pageNo?: number;
    pageSize?: number;
    username?: string;
    role?: string;
    status?: 0 | 1 | 2 | 3;
  }

  /**
   * 当前用户资料响应。
   */
  interface UserProfileVo {
    userId: number;
    username: string;
    nickname: string;
    phone: string;
    email: string;
    roles: string[];
    status: 0 | 1 | 2 | 3;
    newApiUserId?: number;
    newApiUserName?: string;
    currentBalanceAmount?: string;
    usedQuotaAmount?: string;
    totalQuotaAmount?: string;
    quota?: number;
    usedQuota?: number;
    totalQuota?: number;
    quotaPerUnit?: number;
  }

  /**
   * 当前用户资料更新请求。
   */
  interface UserProfileUpdateRequest {
    nickname: string;
    phone: string;
    email: string;
  }

  /**
   * 当前用户密码修改请求。
   */
  interface UserPasswordUpdateRequest {
    oldPassword: string;
    newPassword: string;
  }

  /**
   * 管理员密码重置请求。
   */
  interface AdminPasswordResetRequest {
    newPassword: string;
  }

  /**
   * 管理员设置用户金额请求。
   */
  interface UserAmountUpdateRequest {
    mode: 'add' | 'subtract' | 'override';
    amount: number;
  }

  /**
   * 管理员创建用户请求。
   */
  interface AdminUserCreateRequest {
    username: string;
    password: string;
    nickname?: string;
    phone?: string;
    email?: string;
    bindNewApi?: boolean;
  }

  /**
   * 管理员查询用户详情响应。
   */
  interface AdminUserDetailVo {
    userId: number;
    username: string;
    nickname: string;
    phone: string;
    email: string;
    roles: string[];
    status: 0 | 1 | 2 | 3;
    newApiBound: boolean;
    newApiUserId?: number;
    newApiUserName?: string;
    newApiStatus?: number;
    currentBalanceAmount?: string;
    usedQuotaAmount?: string;
    totalQuotaAmount?: string;
    quota?: number;
    usedQuota?: number;
    totalQuota?: number;
    quotaPerUnit?: number;
  }

  /**
   * 用户 Token 使用记录查询参数。
   */
  interface UserTokenRecordsQuery {
    pageNo?: number;
    pageSize?: number;
    startTime?: string;
    endTime?: string;
    modelName?: string;
  }

  /**
   * 管理员创建用户响应。
   */
  interface UserCreateResponse {
    userId: number;
    username: string;
    newApiBound: boolean;
  }

  /**
   * 用户 Token 使用记录单条。
   */
  interface UserTokenRecordItem {
    id: number;
    newApiUserId?: number;
    newApiUserName?: string;
    modelName: string;
    createdAt: number;
    tokenUsed: number;
    count: number;
    quota: number;
  }

  /**
   * 用户 Token 使用记录分页响应。
   */
  interface UserTokenRecordsVo {
    items: UserTokenRecordItem[];
    total: number;
    pageNo: number;
    pageSize: number;
  }

  /**
   * 系统角色。
   */
  interface SysRole {
    roleId: number;
    roleCode: string;
    roleName: string;
    description?: string;
    status: number;
    builtin: boolean;
    sort: number;
  }

  /**
   * 角色保存请求。
   */
  interface RoleSaveRequest {
    roleCode?: string;
    roleName: string;
    description?: string;
    status?: number;
    sort?: number;
  }

  /**
   * 系统菜单。
   */
  interface SysMenu {
    menuId: number;
    parentId: number;
    menuType: 'DIR' | 'MENU' | 'BUTTON' | string;
    menuName: string;
    path?: string;
    componentKey?: string;
    permissionCode?: string;
    icon?: string;
    visible: boolean;
    status: number;
    builtin: boolean;
    sort: number;
  }

  /**
   * 菜单保存请求。
   */
  interface MenuSaveRequest {
    parentId?: number;
    menuType: string;
    menuName: string;
    path?: string;
    componentKey?: string;
    permissionCode?: string;
    icon?: string;
    visible?: boolean;
    status?: number;
    sort?: number;
  }

  /**
   * 系统应用。
   */
  interface SysApp {
    appId: number;
    appCode: string;
    appName: string;
    description?: string;
    status: number;
    sort: number;
    permissionCount?: number;
  }

  /**
   * 系统应用保存请求。
   */
  interface AppSaveRequest {
    appCode?: string;
    appName: string;
    description?: string;
    status?: number;
    sort?: number;
  }

  /**
   * API权限。
   */
  interface SysApiPermission {
    apiPermissionId: number;
    appId: number;
    permissionCode: string;
    permissionName: string;
    method: string;
    pathPattern: string;
    description?: string;
    status: number;
    builtin: boolean;
    sort: number;
  }

  /**
   * API权限保存请求。
   */
  interface ApiPermissionSaveRequest {
    appId: number;
    permissionCode?: string;
    permissionName: string;
    method: string;
    pathPattern: string;
    description?: string;
    status?: number;
    sort?: number;
  }

  /**
   * 角色授权响应。
   */
  interface RoleGrantVo {
    menuIds: number[];
    apiPermissionIds: number[];
  }
}
