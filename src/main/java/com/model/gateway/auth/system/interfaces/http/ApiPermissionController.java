package com.model.gateway.auth.system.interfaces.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.model.gateway.auth.system.application.RbacApplicationService;
import com.model.gateway.auth.system.domain.model.SysApiPermission;
import com.model.gateway.auth.system.interfaces.dto.ApiPermissionSaveRequest;
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
 * API权限管理接口控制器。
 */
@RestController
@RequestMapping("/api/admin/rbac/api-permissions")
public class ApiPermissionController {

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * 创建API权限管理接口控制器。
     *
     * @param rbacApplicationService RBAC应用服务
     */
    public ApiPermissionController(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
    }

    /**
     * 查询API权限列表。
     *
     * @return API权限列表
     */
    @SaCheckPermission(RbacPermissionConstants.API_PERMISSION_VIEW)
    @GetMapping
    public ApiResponse<List<SysApiPermission>> listApiPermissions() {
        return ApiResponse.success(rbacApplicationService.listApiPermissions());
    }

    /**
     * 创建API权限。
     *
     * @param request API权限保存请求
     * @return API权限实体
     */
    @SaCheckPermission(RbacPermissionConstants.API_PERMISSION_WRITE)
    @PostMapping
    public ApiResponse<SysApiPermission> createApiPermission(@RequestBody ApiPermissionSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.createApiPermission(request));
    }

    /**
     * 更新API权限。
     *
     * @param apiPermissionId API权限ID
     * @param request API权限保存请求
     * @return API权限实体
     */
    @SaCheckPermission(RbacPermissionConstants.API_PERMISSION_WRITE)
    @PutMapping("/{apiPermissionId}")
    public ApiResponse<SysApiPermission> updateApiPermission(
            @PathVariable Long apiPermissionId,
            @RequestBody ApiPermissionSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.updateApiPermission(apiPermissionId, request));
    }

    /**
     * 删除API权限。
     *
     * @param apiPermissionId API权限ID
     * @return 删除结果
     */
    @SaCheckPermission(RbacPermissionConstants.API_PERMISSION_WRITE)
    @DeleteMapping("/{apiPermissionId}")
    public ApiResponse<Boolean> deleteApiPermission(@PathVariable Long apiPermissionId) {
        rbacApplicationService.deleteApiPermission(apiPermissionId);
        return ApiResponse.success(Boolean.TRUE);
    }
}
