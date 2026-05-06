package com.model.gateway.auth.rbac.interfaces.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.model.gateway.auth.rbac.application.RbacApplicationService;
import com.model.gateway.auth.rbac.domain.model.SysMenu;
import com.model.gateway.auth.rbac.interfaces.dto.MenuSaveRequest;
import com.model.gateway.auth.rbac.interfaces.vo.MenuTreeVo;
import com.model.gateway.auth.rbac.shared.RbacPermissionConstants;
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
 * 菜单管理接口控制器。
 */
@RestController
@RequestMapping("/api/admin/rbac/menus")
public class MenuController {

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * 创建菜单管理接口控制器。
     *
     * @param rbacApplicationService RBAC应用服务
     */
    public MenuController(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
    }

    /**
     * 查询菜单树。
     *
     * @return 菜单树
     */
    @SaCheckPermission(RbacPermissionConstants.MENU_VIEW)
    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeVo>> listMenuTree() {
        return ApiResponse.success(rbacApplicationService.listMenuTree());
    }

    /**
     * 查询菜单列表。
     *
     * @return 菜单列表
     */
    @SaCheckPermission(RbacPermissionConstants.MENU_VIEW)
    @GetMapping
    public ApiResponse<List<SysMenu>> listMenus() {
        return ApiResponse.success(rbacApplicationService.listMenus());
    }

    /**
     * 创建菜单。
     *
     * @param request 菜单保存请求
     * @return 菜单实体
     */
    @SaCheckPermission(RbacPermissionConstants.MENU_WRITE)
    @PostMapping
    public ApiResponse<SysMenu> createMenu(@RequestBody MenuSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.createMenu(request));
    }

    /**
     * 更新菜单。
     *
     * @param menuId 菜单ID
     * @param request 菜单保存请求
     * @return 菜单实体
     */
    @SaCheckPermission(RbacPermissionConstants.MENU_WRITE)
    @PutMapping("/{menuId}")
    public ApiResponse<SysMenu> updateMenu(@PathVariable Long menuId, @RequestBody MenuSaveRequest request) {
        return ApiResponse.success(rbacApplicationService.updateMenu(menuId, request));
    }

    /**
     * 删除菜单。
     *
     * @param menuId 菜单ID
     * @return 删除结果
     */
    @SaCheckPermission(RbacPermissionConstants.MENU_WRITE)
    @DeleteMapping("/{menuId}")
    public ApiResponse<Boolean> deleteMenu(@PathVariable Long menuId) {
        rbacApplicationService.deleteMenu(menuId);
        return ApiResponse.success(Boolean.TRUE);
    }
}
