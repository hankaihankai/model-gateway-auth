import { request } from '@umijs/max';
import type { GeographicItemType } from './data';
import { getCurrentUserProfile } from '@/services/user';

/**
 * 兼容统一响应包装和已解包数据。
 */
const unwrapApiData = <T,>(response: T | API.ApiResponse<T>): T => {
  return ((response as API.ApiResponse<T>)?.data ?? response) as T;
};

export async function queryCurrent(): Promise<API.UserProfileVo> {
  const response = await getCurrentUserProfile();
  return unwrapApiData<API.UserProfileVo>(response as API.UserProfileVo | API.ApiResponse<API.UserProfileVo>);
}

export async function queryProvince(): Promise<{ data: GeographicItemType[] }> {
  return request('/api/geographic/province');
}

export async function queryCity(
  province: string,
): Promise<{ data: GeographicItemType[] }> {
  return request(`/api/geographic/city/${province}`);
}

export async function query() {
  return request('/api/users');
}
