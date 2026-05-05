import { request } from '@umijs/max';

/**
 * 查询用户列表（开发期走 mock，部署期走真实后端）。
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
 * 管理员测试用户AI调用。
 */
export async function testAiCall(userId: number, body: API.TestAiCallRequest) {
  return request<API.TestAiCallResponse>(`/model-gateway-auth/api/admin/users/${userId}/test-ai-call`, {
    method: 'POST',
    data: body,
  });
}
