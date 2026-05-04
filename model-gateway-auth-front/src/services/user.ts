import { request } from '@umijs/max';

/**
 * 查询用户列表（开发期走 mock，部署期走真实后端）。
 */
export async function listUsers(query: API.UserListQuery) {
  return request<API.ApiResponse<API.UserListPageData>>('/model-gateway-auth/api/admin/users', {
    method: 'GET',
    params: query,
  });
}

/**
 * 管理员创建用户。
 */
export async function createUser(body: API.AdminUserCreateRequest) {
  return request<API.ApiResponse<API.UserCreateResponse>>('/model-gateway-auth/api/admin/users', {
    method: 'POST',
    data: body,
  });
}

/**
 * 管理员查询用户详情。
 */
export async function getUserDetail(userId: number) {
  return request<API.ApiResponse<API.AdminUserDetailVo>>(`/model-gateway-auth/api/admin/users/${userId}`, {
    method: 'GET',
  });
}

/**
 * 管理员查询用户Token使用记录。
 */
export async function getUserTokenRecords(userId: number, params: API.UserTokenRecordsQuery) {
  return request<API.ApiResponse<API.UserTokenRecordsVo>>(`/model-gateway-auth/api/admin/users/${userId}/token-records`, {
    method: 'GET',
    params,
  });
}
