import { request } from '@umijs/max';

/**
 * 查询角色列表。
 */
export async function listRoles() {
  return request<API.ApiResponse<API.SysRole[]>>('/model-gateway-auth/api/admin/rbac/roles', {
    method: 'GET',
  });
}

/**
 * 创建角色。
 */
export async function createRole(body: API.RoleSaveRequest) {
  return request<API.ApiResponse<API.SysRole>>('/model-gateway-auth/api/admin/rbac/roles', {
    method: 'POST',
    data: body,
  });
}

/**
 * 更新角色。
 */
export async function updateRole(roleId: number, body: API.RoleSaveRequest) {
  return request<API.ApiResponse<API.SysRole>>(`/model-gateway-auth/api/admin/rbac/roles/${roleId}`, {
    method: 'PUT',
    data: body,
  });
}

/**
 * 删除角色。
 */
export async function deleteRole(roleId: number) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/rbac/roles/${roleId}`, {
    method: 'DELETE',
  });
}

/**
 * 查询角色授权。
 */
export async function getRoleGrant(roleId: number) {
  return request<API.ApiResponse<API.RoleGrantVo>>(`/model-gateway-auth/api/admin/rbac/roles/${roleId}/grant`, {
    method: 'GET',
  });
}

/**
 * 更新角色授权。
 */
export async function updateRoleGrant(roleId: number, body: { menuIds: number[]; apiPermissionIds: number[] }) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/rbac/roles/${roleId}/grant`, {
    method: 'PUT',
    data: body,
  });
}

/**
 * 查询菜单列表。
 */
export async function listMenus() {
  return request<API.ApiResponse<API.SysMenu[]>>('/model-gateway-auth/api/admin/rbac/menus', {
    method: 'GET',
  });
}

/**
 * 创建菜单。
 */
export async function createMenu(body: API.MenuSaveRequest) {
  return request<API.ApiResponse<API.SysMenu>>('/model-gateway-auth/api/admin/rbac/menus', {
    method: 'POST',
    data: body,
  });
}

/**
 * 更新菜单。
 */
export async function updateMenu(menuId: number, body: API.MenuSaveRequest) {
  return request<API.ApiResponse<API.SysMenu>>(`/model-gateway-auth/api/admin/rbac/menus/${menuId}`, {
    method: 'PUT',
    data: body,
  });
}

/**
 * 删除菜单。
 */
export async function deleteMenu(menuId: number) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/rbac/menus/${menuId}`, {
    method: 'DELETE',
  });
}

/**
 * 查询系统应用列表。
 */
export async function listApps() {
  return request<API.ApiResponse<API.SysApp[]>>('/model-gateway-auth/api/admin/rbac/apps', {
    method: 'GET',
  });
}

/**
 * 创建系统应用。
 */
export async function createApp(body: API.AppSaveRequest) {
  return request<API.ApiResponse<API.SysApp>>('/model-gateway-auth/api/admin/rbac/apps', {
    method: 'POST',
    data: body,
  });
}

/**
 * 更新系统应用。
 */
export async function updateApp(appId: number, body: API.AppSaveRequest) {
  return request<API.ApiResponse<API.SysApp>>(`/model-gateway-auth/api/admin/rbac/apps/${appId}`, {
    method: 'PUT',
    data: body,
  });
}

/**
 * 删除系统应用。
 */
export async function deleteApp(appId: number) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/rbac/apps/${appId}`, {
    method: 'DELETE',
  });
}

/**
 * 查询API权限列表。
 */
export async function listApiPermissions() {
  return request<API.ApiResponse<API.SysApiPermission[]>>('/model-gateway-auth/api/admin/rbac/api-permissions', {
    method: 'GET',
  });
}

/**
 * 创建API权限。
 */
export async function createApiPermission(body: API.ApiPermissionSaveRequest) {
  return request<API.ApiResponse<API.SysApiPermission>>('/model-gateway-auth/api/admin/rbac/api-permissions', {
    method: 'POST',
    data: body,
  });
}

/**
 * 更新API权限。
 */
export async function updateApiPermission(apiPermissionId: number, body: API.ApiPermissionSaveRequest) {
  return request<API.ApiResponse<API.SysApiPermission>>(
    `/model-gateway-auth/api/admin/rbac/api-permissions/${apiPermissionId}`,
    {
      method: 'PUT',
      data: body,
    },
  );
}

/**
 * 删除API权限。
 */
export async function deleteApiPermission(apiPermissionId: number) {
  return request<API.ApiResponse<boolean>>(`/model-gateway-auth/api/admin/rbac/api-permissions/${apiPermissionId}`, {
    method: 'DELETE',
  });
}
