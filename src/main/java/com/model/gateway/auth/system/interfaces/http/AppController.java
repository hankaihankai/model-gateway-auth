package com.model.gateway.auth.system.interfaces.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.model.gateway.auth.shared.api.ApiResponse;
import com.model.gateway.auth.system.application.RbacApplicationService;
import com.model.gateway.auth.system.domain.model.SysApp;
import com.model.gateway.auth.system.interfaces.dto.AppSaveRequest;
import com.model.gateway.auth.system.interfaces.vo.SysAppVo;
import com.model.gateway.auth.system.shared.RbacPermissionConstants;
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
 * 系统应用管理接口控制器。
 */
@RestController
@RequestMapping("/api/admin/rbac/apps")
public class AppController {

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * 创建系统应用管理接口控制器。
     *
     * @param rbacApplicationService RBAC应用服务
     */
    public AppController(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
    }

    /**
     * 查询系统应用列表。
     *
     * @return 系统应用列表
     */
    @SaCheckPermission(value = {RbacPermissionConstants.APP_VIEW, RbacPermissionConstants.API_PERMISSION_VIEW}, mode = SaMode.OR)
    @GetMapping
    public ApiResponse<List<SysAppVo>> listApps() {
        return ApiResponse.success(rbacApplicationService.listApps());
    }

    /**
     * 创建系统应用。
     *
     * @param request 应用保存请求
     * @return 系统应用实体
     */
    @SaCheckPermission(value = {RbacPermissionConstants.APP_WRITE, RbacPermissionConstants.API_PERMISSION_WRITE}, mode = SaMode.OR)
    @PostMapping
    public ApiResponse<SysApp> createApp(@RequestBody AppSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.createApp(request));
    }

    /**
     * 更新系统应用。
     *
     * @param appId 应用ID
     * @param request 应用保存请求
     * @return 系统应用实体
     */
    @SaCheckPermission(value = {RbacPermissionConstants.APP_WRITE, RbacPermissionConstants.API_PERMISSION_WRITE}, mode = SaMode.OR)
    @PutMapping("/{appId}")
    public ApiResponse<SysApp> updateApp(@PathVariable Long appId, @RequestBody AppSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.updateApp(appId, request));
    }

    /**
     * 删除系统应用。
     *
     * @param appId 应用ID
     * @return 删除结果
     */
    @SaCheckPermission(value = {RbacPermissionConstants.APP_WRITE, RbacPermissionConstants.API_PERMISSION_WRITE}, mode = SaMode.OR)
    @DeleteMapping("/{appId}")
    public ApiResponse<Boolean> deleteApp(@PathVariable Long appId) {
        rbacApplicationService.deleteApp(appId);
        return ApiResponse.success(Boolean.TRUE);
    }
}
