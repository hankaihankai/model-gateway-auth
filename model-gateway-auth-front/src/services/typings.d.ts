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
  }

  /**
   * 当前登录用户简要信息。
   */
  interface UserInfo {
    userId: number;
    username: string;
    nickname: string;
    role: 'ADMIN' | 'USER' | string;
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
    role: 'ADMIN' | 'USER';
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
    role?: 'ADMIN' | 'USER';
    status?: 0 | 1 | 2 | 3;
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
    role: 'ADMIN' | 'USER';
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
}
