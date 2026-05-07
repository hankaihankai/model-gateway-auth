import { request } from '@umijs/max';

/**
 * 查询用户列表。
 */
export async function listUsers(query: API.UserListQuery) {
  return request<API.UserListPageData>('/model-gateway-auth/api/admin/users', {
    method: 'GET',
    params: query,
  });
}

/**
 * 管理员创建用户。
 */
export async function createUser(body: API.AdminUserCreateRequest) {
  return request<API.UserCreateResponse>('/model-gateway-auth/api/admin/users', {
    method: 'POST',
    data: body,
  });
}

/**
 * 管理员查询用户详情。
 */
export async function getUserDetail(userId: number) {
  return request<API.AdminUserDetailVo>(`/model-gateway-auth/api/admin/users/${userId}`, {
    method: 'GET',
  });
}

/**
 * 查询当前用户资料。
 */
export async function getCurrentUserProfile() {
  return request<API.UserProfileVo>('/model-gateway-auth/api/user/profile', {
    method: 'GET',
  });
}

/**
 * 更新当前用户资料。
 */
export async function updateCurrentUserProfile(body: API.UserProfileUpdateRequest) {
  return request<boolean>('/model-gateway-auth/api/user/profile', {
    method: 'PUT',
    data: body,
  });
}

/**
 * 修改当前用户密码。
 */
export async function updateCurrentUserPassword(body: API.UserPasswordUpdateRequest) {
  return request<boolean>('/model-gateway-auth/api/user/password', {
    method: 'PUT',
    data: body,
  });
}

/**
 * 管理员重置用户密码。
 */
export async function resetUserPassword(userId: number, body: API.AdminPasswordResetRequest) {
  return request<boolean>(`/model-gateway-auth/api/admin/users/${userId}/password/reset`, {
    method: 'POST',
    data: body,
  });
}

/**
 * 管理员查询用户Token使用记录。
 */
export async function getUserTokenRecords(userId: number, params: API.UserTokenRecordsQuery) {
  return request<API.UserTokenRecordsVo>(`/model-gateway-auth/api/admin/users/${userId}/token-records`, {
    method: 'GET',
    params,
  });
}

/**
 * 管理员为已有用户补绑 new-api。
 */
export async function bindNewApi(userId: number) {
  return request<void>(`/model-gateway-auth/api/admin/users/${userId}/bind-new-api`, {
    method: 'POST',
  });
}

/**
 * 管理员修改用户状态。
 */
export async function updateUserStatus(userId: number, status: number) {
  return request<void>(`/model-gateway-auth/api/admin/users/${userId}/status`, {
    method: 'POST',
    params: { status },
  });
}

/**
 * 管理员设置用户金额。
 */
export async function updateUserAmount(userId: number, body: API.UserAmountUpdateRequest) {
  return request<void>(`/model-gateway-auth/api/admin/users/${userId}/amount`, {
    method: 'POST',
    data: body,
  });
}

/**
 * 管理员查询用户可用模型。
 */
export async function getUserModels(userId: number) {
  return request<string[]>(`/model-gateway-auth/api/admin/users/${userId}/models`, {
    method: 'GET',
  });
}

/**
 * 查询用户角色ID列表。
 */
export async function getUserRoles(userId: number) {
  return request<API.ApiResponse<number[]>>(`/model-gateway-auth/api/admin/users/${userId}/roles`, {
    method: 'GET',
  });
}

/**
 * 更新用户角色。
 */
export async function updateUserRoles(userId: number, roleIds: number[]) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/users/${userId}/roles`, {
    method: 'PUT',
    data: { roleIds },
  });
}
