import { request } from '@umijs/max';

/**
 * 用户登录。
 */
export async function login(body: { username: string; password: string }) {
  return request<API.ApiResponse<API.LoginResponse>>('/model-gateway-auth/api/auth/login', {
    method: 'POST',
    data: body,
    skipErrorHandler: true, // 失败时由登录页自己处理 message
  });
}

/**
 * 刷新 Token（本期不调用，留接口位）。
 */
export async function refreshToken() {
  return request<API.ApiResponse<API.LoginResponse>>('/model-gateway-auth/api/auth/refresh', {
    method: 'POST',
  });
}

/**
 * 查询当前登录上下文。
 */
export async function current() {
  return request<API.ApiResponse<API.LoginResponse>>('/model-gateway-auth/api/auth/current', {
    method: 'GET',
  });
}

/**
 * 用户登出。
 */
export async function logout() {
  return request<API.ApiResponse<boolean>>('/model-gateway-auth/api/auth/logout', {
    method: 'POST',
    skipErrorHandler: true, // 登出失败也照样清登录态
  });
}
