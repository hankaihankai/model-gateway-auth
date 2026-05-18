package com.model.gateway.auth.system.interfaces.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.model.gateway.auth.system.application.RbacApplicationService;
import com.model.gateway.auth.system.domain.model.SysRole;
import com.model.gateway.auth.system.interfaces.dto.RoleGrantRequest;
import com.model.gateway.auth.system.interfaces.dto.RoleSaveRequest;
import com.model.gateway.auth.system.interfaces.vo.RoleGrantVo;
import com.model.gateway.auth.system.shared.RbacPermissionConstants;
import com.model.gateway.auth.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口控制器。
 */
@RestController
@RequestMapping("/api/admin/rbac/roles")
public class RoleController {

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * 创建角色管理接口控制器。
     *
     * @param rbacApplicationService RBAC应用服务
     */
    public RoleController(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
    }

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_VIEW)
    @GetMapping
    public ApiResponse<List<SysRole>> listRoles() {
        return ApiResponse.success(rbacApplicationService.listRoles());
    }

    /**
     * 创建角色。
     *
     * @param request 角色保存请求
     * @return 角色实体
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_WRITE)
    @PostMapping
    public ApiResponse<SysRole> createRole(@RequestBody RoleSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.createRole(request));
    }

    /**
     * 更新角色。
     *
     * @param roleId 角色ID
     * @param request 角色保存请求
     * @return 角色实体
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_WRITE)
    @PutMapping("/{roleId}")
    public ApiResponse<SysRole> updateRole(@PathVariable Long roleId, @RequestBody RoleSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.updateRole(roleId, request));
    }

    /**
     * 删除角色。
     *
     * @param roleId 角色ID
     * @return 删除结果
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_WRITE)
    @DeleteMapping("/{roleId}")
    public ApiResponse<Boolean> deleteRole(@PathVariable Long roleId) {
        rbacApplicationService.deleteRole(roleId);
        return ApiResponse.success(Boolean.TRUE);
    }

    /**
     * 查询角色授权。
     *
     * @param roleId 角色ID
     * @return 角色授权
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_VIEW)
    @GetMapping("/{roleId}/grant")
    public ApiResponse<RoleGrantVo> getRoleGrant(@PathVariable Long roleId) {
        return ApiResponse.success(rbacApplicationService.getRoleGrant(roleId));
    }

    /**
     * 更新角色授权。
     *
     * @param roleId 角色ID
     * @param request 角色授权请求
     * @return 更新结果
     */
    @SaCheckPermission(RbacPermissionConstants.ROLE_WRITE)
    @PutMapping("/{roleId}/grant")
    public ApiResponse<Boolean> updateRoleGrant(@PathVariable Long roleId, @RequestBody RoleGrantRequest request) {
        rbacApplicationService.updateRoleGrant(roleId, request);
        return ApiResponse.success(Boolean.TRUE);
    }
}
