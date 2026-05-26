package com.model.gateway.auth.system.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.model.gateway.auth.system.domain.model.SysApp;
import com.model.gateway.auth.system.domain.model.SysUser;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysAppMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysUserMapper;
import com.model.gateway.auth.system.domain.model.SysApiPermission;
import com.model.gateway.auth.system.domain.model.SysMenu;
import com.model.gateway.auth.system.domain.model.SysRole;
import com.model.gateway.auth.system.domain.model.SysRoleApiPermission;
import com.model.gateway.auth.system.domain.model.SysRoleMenu;
import com.model.gateway.auth.system.domain.model.SysUserRole;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysApiPermissionMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysMenuMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysRoleApiPermissionMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysRoleMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.model.gateway.auth.system.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.model.gateway.auth.system.interfaces.dto.AppSaveRequest;
import com.model.gateway.auth.system.interfaces.dto.ApiPermissionSaveRequest;
import com.model.gateway.auth.system.interfaces.dto.MenuSaveRequest;
import com.model.gateway.auth.system.interfaces.dto.RoleGrantRequest;
import com.model.gateway.auth.system.interfaces.dto.RoleSaveRequest;
import com.model.gateway.auth.system.interfaces.dto.UserRoleUpdateRequest;
import com.model.gateway.auth.system.interfaces.vo.MenuTreeVo;
import com.model.gateway.auth.system.interfaces.vo.PermissionContextVo;
import com.model.gateway.auth.system.interfaces.vo.RoleGrantVo;
import com.model.gateway.auth.system.interfaces.vo.SysAppVo;
import com.model.gateway.auth.shared.enums.UserStatusEnum;
import com.model.gateway.auth.shared.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RBAC应用服务。
 */
@Service
public class RbacApplicationService {

    /**
     * 默认普通用户角色编码。
     */
    private static final String DEFAULT_USER_ROLE_CODE = "USER";

    /**
     * 菜单根节点父ID。
     */
    private static final long ROOT_PARENT_ID = 0L;

    /**
     * 全部HTTP方法通配符。
     */
    private static final String ALL_METHODS = "*";

    /**
     * 路径匹配器。
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 系统应用数据访问对象。
     */
    private final SysAppMapper appMapper;

    /**
     * 系统角色数据访问对象。
     */
    private final SysRoleMapper roleMapper;

    /**
     * 系统菜单数据访问对象。
     */
    private final SysMenuMapper menuMapper;

    /**
     * 系统API权限数据访问对象。
     */
    private final SysApiPermissionMapper apiPermissionMapper;

    /**
     * 用户角色关联数据访问对象。
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 角色菜单关联数据访问对象。
     */
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * 角色API权限关联数据访问对象。
     */
    private final SysRoleApiPermissionMapper roleApiPermissionMapper;

    /**
     * 用户数据访问对象。
     */
    private final SysUserMapper sysUserMapper;

    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建RBAC应用服务。
     *
     * @param appMapper 系统应用数据访问对象
     * @param roleMapper 系统角色数据访问对象
     * @param menuMapper 系统菜单数据访问对象
     * @param apiPermissionMapper 系统API权限数据访问对象
     * @param userRoleMapper 用户角色关联数据访问对象
     * @param roleMenuMapper 角色菜单关联数据访问对象
     * @param roleApiPermissionMapper 角色API权限关联数据访问对象
     * @param sysUserMapper 用户数据访问对象
     * @param transactionTemplate 事务模板
     */
    public RbacApplicationService(
            SysAppMapper appMapper,
            SysRoleMapper roleMapper,
            SysMenuMapper menuMapper,
            SysApiPermissionMapper apiPermissionMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysRoleApiPermissionMapper roleApiPermissionMapper,
            SysUserMapper sysUserMapper,
            TransactionTemplate transactionTemplate) {
        this.appMapper = appMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.apiPermissionMapper = apiPermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleApiPermissionMapper = roleApiPermissionMapper;
        this.sysUserMapper = sysUserMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 查询当前用户权限上下文。
     *
     * @param userId 用户ID
     * @return 权限上下文
     */
    public PermissionContextVo getPermissionContext(Long userId) {
        return PermissionContextVo.builder()
                .roles(getRoleCodes(userId))
                .permissions(getPermissionCodes(userId))
                .menus(buildMenuTree(menuMapper.selectMenusByUserId(userId), true))
                .build();
    }

    /**
     * 查询用户角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    public List<String> getRoleCodes(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 查询用户权限编码列表。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    public List<String> getPermissionCodes(Long userId) {
        return apiPermissionMapper.selectPermissionCodesByUserId(userId);
    }

    /**
     * 为用户绑定默认普通用户角色。
     *
     * @param userId 用户ID
     */
    public void assignDefaultUserRole(Long userId) {
        SysRole role = selectRoleByCode(DEFAULT_USER_ROLE_CODE);
        if (role != null) {
            userRoleMapper.insert(SysUserRole.builder()
                    .userId(userId)
                    .roleId(role.getRoleId())
                    .build());
        }
    }

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    public List<SysRole> listRoles() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .orderByAsc(SysRole::getSort, SysRole::getRoleId));
    }

    /**
     * 创建角色。
     *
     * @param request 角色保存请求
     * @return 角色实体
     */
    public SysRole createRole(RoleSaveRequest request) {
        checkRoleRequest(request, true);
        if (selectRoleByCode(request.getRoleCode()) != null) {
            throw new AuthException("角色编码已存在");
        }
        SysRole role = SysRole.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .status(resolveStatus(request.getStatus()))
                .builtin(Boolean.FALSE)
                .sort(resolveSort(request.getSort()))
                .build();
        roleMapper.insert(role);
        return role;
    }

    /**
     * 更新角色。
     *
     * @param roleId 角色ID
     * @param request 角色保存请求
     * @return 角色实体
     */
    public SysRole updateRole(Long roleId, RoleSaveRequest request) {
        SysRole exists = requireRole(roleId);
        checkRoleRequest(request, false);
        exists.setRoleName(request.getRoleName());
        exists.setDescription(request.getDescription());
        exists.setStatus(resolveStatus(request.getStatus()));
        exists.setSort(resolveSort(request.getSort()));
        roleMapper.update(null, Wrappers.<SysRole>lambdaUpdate()
                .set(SysRole::getRoleName, exists.getRoleName())
                .set(SysRole::getDescription, exists.getDescription())
                .set(SysRole::getStatus, exists.getStatus())
                .set(SysRole::getSort, exists.getSort())
                .eq(SysRole::getRoleId, roleId));
        return exists;
    }

    /**
     * 删除角色。
     *
     * @param roleId 角色ID
     */
    public void deleteRole(Long roleId) {
        SysRole role = requireRole(roleId);
        checkNotBuiltin(role.getBuiltin(), "内置角色不能删除");
        int rows = roleMapper.delete(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleId, roleId)
                .eq(SysRole::getBuiltin, Boolean.FALSE));
        if (rows == 0) {
            throw new AuthException("角色删除失败");
        }
    }

    /**
     * 查询角色授权。
     *
     * @param roleId 角色ID
     * @return 角色授权响应
     */
    public RoleGrantVo getRoleGrant(Long roleId) {
        requireRole(roleId);
        return RoleGrantVo.builder()
                .menuIds(roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                                .eq(SysRoleMenu::getRoleId, roleId))
                        .stream()
                        .map(SysRoleMenu::getMenuId)
                        .toList())
                .apiPermissionIds(roleApiPermissionMapper.selectList(Wrappers.<SysRoleApiPermission>lambdaQuery()
                                .eq(SysRoleApiPermission::getRoleId, roleId))
                        .stream()
                        .map(SysRoleApiPermission::getApiPermissionId)
                        .toList())
                .build();
    }

    /**
     * 更新角色授权。
     *
     * @param roleId 角色ID
     * @param request 角色授权请求
     */
    public void updateRoleGrant(Long roleId, RoleGrantRequest request) {
        requireRole(roleId);
        List<Long> menuIds = request == null || request.getMenuIds() == null
                ? Collections.emptyList()
                : request.getMenuIds();
        List<Long> apiPermissionIds = request == null || request.getApiPermissionIds() == null
                ? Collections.emptyList()
                : request.getApiPermissionIds();
        transactionTemplate.executeWithoutResult(status -> {
            roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery()
                    .eq(SysRoleMenu::getRoleId, roleId));
            for (Long menuId : menuIds) {
                roleMenuMapper.insert(SysRoleMenu.builder()
                        .roleId(roleId)
                        .menuId(menuId)
                        .build());
            }
            roleApiPermissionMapper.delete(Wrappers.<SysRoleApiPermission>lambdaQuery()
                    .eq(SysRoleApiPermission::getRoleId, roleId));
            for (Long apiPermissionId : apiPermissionIds) {
                roleApiPermissionMapper.insert(SysRoleApiPermission.builder()
                        .roleId(roleId)
                        .apiPermissionId(apiPermissionId)
                        .build());
            }
        });
    }

    /**
     * 查询菜单树。
     *
     * @return 菜单树
     */
    public List<MenuTreeVo> listMenuTree() {
        return buildMenuTree(selectAllMenus(), false);
    }

    /**
     * 查询菜单平铺列表。
     *
     * @return 菜单列表
     */
    public List<SysMenu> listMenus() {
        return selectAllMenus();
    }

    /**
     * 创建菜单。
     *
     * @param request 菜单保存请求
     * @return 菜单实体
     */
    public SysMenu createMenu(MenuSaveRequest request) {
        checkMenuRequest(request);
        SysMenu menu = buildMenu(null, request, Boolean.FALSE);
        menuMapper.insert(menu);
        return menu;
    }

    /**
     * 更新菜单。
     *
     * @param menuId 菜单ID
     * @param request 菜单保存请求
     * @return 菜单实体
     */
    public SysMenu updateMenu(Long menuId, MenuSaveRequest request) {
        SysMenu exists = requireMenu(menuId);
        checkMenuRequest(request);
        SysMenu menu = buildMenu(menuId, request, exists.getBuiltin());
        menuMapper.update(null, Wrappers.<SysMenu>lambdaUpdate()
                .set(SysMenu::getParentId, menu.getParentId())
                .set(SysMenu::getMenuType, menu.getMenuType())
                .set(SysMenu::getMenuName, menu.getMenuName())
                .set(SysMenu::getPath, menu.getPath())
                .set(SysMenu::getComponentKey, menu.getComponentKey())
                .set(SysMenu::getPermissionCode, menu.getPermissionCode())
                .set(SysMenu::getIcon, menu.getIcon())
                .set(SysMenu::getVisible, menu.getVisible())
                .set(SysMenu::getStatus, menu.getStatus())
                .set(SysMenu::getSort, menu.getSort())
                .eq(SysMenu::getMenuId, menuId));
        return menu;
    }

    /**
     * 删除菜单。
     *
     * @param menuId 菜单ID
     */
    public void deleteMenu(Long menuId) {
        SysMenu menu = requireMenu(menuId);
        checkNotBuiltin(menu.getBuiltin(), "内置菜单不能删除");
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getParentId, menuId)) > 0) {
            throw new AuthException("存在子菜单，不能删除");
        }
        int rows = menuMapper.delete(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getMenuId, menuId)
                .eq(SysMenu::getBuiltin, Boolean.FALSE));
        if (rows == 0) {
            throw new AuthException("菜单删除失败");
        }
    }

    /**
     * 查询API权限列表。
     *
     * @return API权限列表
     */
    public List<SysApiPermission> listApiPermissions() {
        return apiPermissionMapper.selectAllByAppOrder();
    }

    /**
     * 查询系统应用列表。
     *
     * @return 系统应用列表
     */
    public List<SysAppVo> listApps() {
        return appMapper.selectList(Wrappers.<SysApp>lambdaQuery()
                        .orderByAsc(SysApp::getSort, SysApp::getAppId))
                .stream()
                .map(this::toSysAppVo)
                .toList();
    }

    /**
     * 创建系统应用。
     *
     * @param request 应用保存请求
     * @return 系统应用实体
     */
    public SysApp createApp(AppSaveRequest request) {
        checkAppRequest(request, true);
        if (selectAppByCode(request.getAppCode()) != null) {
            throw new AuthException("应用编码已存在");
        }
        SysApp app = buildApp(null, request);
        appMapper.insert(app);
        return app;
    }

    /**
     * 更新系统应用。
     *
     * @param appId 应用ID
     * @param request 应用保存请求
     * @return 系统应用实体
     */
    public SysApp updateApp(Long appId, AppSaveRequest request) {
        SysApp exists = requireApp(appId);
        checkAppRequest(request, false);
        if (StringUtils.hasText(request.getAppCode()) && !request.getAppCode().equals(exists.getAppCode())) {
            throw new AuthException("应用编码不能修改");
        }
        SysApp app = buildApp(appId, request);
        app.setAppCode(exists.getAppCode());
        appMapper.update(null, Wrappers.<SysApp>lambdaUpdate()
                .set(SysApp::getAppName, app.getAppName())
                .set(SysApp::getDescription, app.getDescription())
                .set(SysApp::getStatus, app.getStatus())
                .set(SysApp::getSort, app.getSort())
                .eq(SysApp::getAppId, appId));
        return app;
    }

    /**
     * 删除系统应用。
     *
     * @param appId 应用ID
     */
    public void deleteApp(Long appId) {
        requireApp(appId);
        if (countApiPermissionsByAppId(appId) > 0) {
            throw new AuthException("应用下存在API权限，不能删除");
        }
        int rows = appMapper.delete(Wrappers.<SysApp>lambdaQuery()
                .eq(SysApp::getAppId, appId));
        if (rows == 0) {
            throw new AuthException("应用删除失败");
        }
    }

    /**
     * 创建API权限。
     *
     * @param request API权限保存请求
     * @return API权限实体
     */
    public SysApiPermission createApiPermission(ApiPermissionSaveRequest request) {
        checkApiPermissionRequest(request, true);
        SysApp app = requireEnabledApp(request.getAppId());
        checkPermissionCodePrefix(request.getPermissionCode(), app);
        SysApiPermission permission = buildApiPermission(null, request, Boolean.FALSE);
        apiPermissionMapper.insert(permission);
        return permission;
    }

    /**
     * 更新API权限。
     *
     * @param apiPermissionId API权限ID
     * @param request API权限保存请求
     * @return API权限实体
     */
    public SysApiPermission updateApiPermission(Long apiPermissionId, ApiPermissionSaveRequest request) {
        SysApiPermission exists = requireApiPermission(apiPermissionId);
        checkApiPermissionRequest(request, false);
        SysApp app = requireEnabledApp(request.getAppId());
        if (StringUtils.hasText(request.getPermissionCode()) && !request.getPermissionCode().equals(exists.getPermissionCode())) {
            throw new AuthException("API权限编码不能修改");
        }
        checkPermissionCodePrefix(exists.getPermissionCode(), app);
        SysApiPermission permission = buildApiPermission(apiPermissionId, request, exists.getBuiltin());
        permission.setPermissionCode(exists.getPermissionCode());
        apiPermissionMapper.update(null, Wrappers.<SysApiPermission>lambdaUpdate()
                .set(SysApiPermission::getAppId, permission.getAppId())
                .set(SysApiPermission::getPermissionName, permission.getPermissionName())
                .set(SysApiPermission::getMethod, permission.getMethod())
                .set(SysApiPermission::getPathPattern, permission.getPathPattern())
                .set(SysApiPermission::getDescription, permission.getDescription())
                .set(SysApiPermission::getStatus, permission.getStatus())
                .set(SysApiPermission::getSort, permission.getSort())
                .eq(SysApiPermission::getApiPermissionId, apiPermissionId));
        return permission;
    }

    /**
     * 查询匹配的用户应用API权限编码。
     *
     * @param userId 用户ID
     * @param appCode 应用编码
     * @param method HTTP方法
     * @param path 应用内部路径
     * @return 匹配的权限编码
     */
    public String findMatchedApiPermissionCode(Long userId, String appCode, String method, String path) {
        if (userId == null || !StringUtils.hasText(appCode) || !StringUtils.hasText(method) || !StringUtils.hasText(path)) {
            return null;
        }
        List<SysApiPermission> permissions = apiPermissionMapper.selectEnabledByUserIdAndAppCode(userId, appCode);
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        for (SysApiPermission permission : permissions) {
            if (matchesMethod(permission.getMethod(), normalizedMethod)
                    && matchesPath(permission.getPathPattern(), path)) {
                return permission.getPermissionCode();
            }
        }
        return null;
    }

    /**
     * 删除API权限。
     *
     * @param apiPermissionId API权限ID
     */
    public void deleteApiPermission(Long apiPermissionId) {
        SysApiPermission permission = requireApiPermission(apiPermissionId);
        checkNotBuiltin(permission.getBuiltin(), "内置API权限不能删除");
        int rows = apiPermissionMapper.delete(Wrappers.<SysApiPermission>lambdaQuery()
                .eq(SysApiPermission::getApiPermissionId, apiPermissionId)
                .eq(SysApiPermission::getBuiltin, Boolean.FALSE));
        if (rows == 0) {
            throw new AuthException("API权限删除失败");
        }
    }

    /**
     * 查询用户角色ID列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        checkUserExists(userId);
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
    }

    /**
     * 更新用户角色。
     *
     * @param userId 用户ID
     * @param request 用户角色更新请求
     */
    public void updateUserRoles(Long userId, UserRoleUpdateRequest request) {
        checkUserExists(userId);
        List<Long> roleIds = request == null || request.getRoleIds() == null
                ? Collections.emptyList()
                : request.getRoleIds();
        transactionTemplate.executeWithoutResult(status -> {
            userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, userId));
            for (Long roleId : roleIds) {
                requireRole(roleId);
                userRoleMapper.insert(SysUserRole.builder()
                        .userId(userId)
                        .roleId(roleId)
                        .build());
            }
        });
    }

    /**
     * 构建菜单树。
     *
     * @param menus 菜单列表
     * @param onlyVisible 是否只返回可见菜单
     * @return 菜单树
     */
    private List<MenuTreeVo> buildMenuTree(List<SysMenu> menus, boolean onlyVisible) {
        Map<Long, MenuTreeVo> nodeMap = new LinkedHashMap<>();
        List<MenuTreeVo> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (onlyVisible && Boolean.FALSE.equals(menu.getVisible())) {
                continue;
            }
            MenuTreeVo node = toMenuTreeVo(menu);
            nodeMap.put(node.getMenuId(), node);
        }
        for (MenuTreeVo node : nodeMap.values()) {
            Long parentId = node.getParentId() == null ? ROOT_PARENT_ID : node.getParentId();
            MenuTreeVo parent = nodeMap.get(parentId);
            if (parent == null || ROOT_PARENT_ID == parentId) {
                roots.add(node);
            } else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 转换菜单树节点。
     *
     * @param menu 菜单实体
     * @return 菜单树节点
     */
    private MenuTreeVo toMenuTreeVo(SysMenu menu) {
        return MenuTreeVo.builder()
                .menuId(menu.getMenuId())
                .parentId(menu.getParentId())
                .menuType(menu.getMenuType())
                .menuName(menu.getMenuName())
                .path(menu.getPath())
                .componentKey(menu.getComponentKey())
                .permissionCode(menu.getPermissionCode())
                .icon(menu.getIcon())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .builtin(menu.getBuiltin())
                .sort(menu.getSort())
                .children(new ArrayList<>())
                .build();
    }

    /**
     * 根据请求构建菜单实体。
     *
     * @param menuId 菜单ID
     * @param request 菜单保存请求
     * @param builtin 是否内置
     * @return 菜单实体
     */
    private SysMenu buildMenu(Long menuId, MenuSaveRequest request, Boolean builtin) {
        return SysMenu.builder()
                .menuId(menuId)
                .parentId(request.getParentId() == null ? ROOT_PARENT_ID : request.getParentId())
                .menuType(request.getMenuType())
                .menuName(request.getMenuName())
                .path(request.getPath())
                .componentKey(request.getComponentKey())
                .permissionCode(request.getPermissionCode())
                .icon(request.getIcon())
                .visible(request.getVisible() == null || request.getVisible())
                .status(resolveStatus(request.getStatus()))
                .builtin(builtin)
                .sort(resolveSort(request.getSort()))
                .build();
    }

    /**
     * 根据请求构建API权限实体。
     *
     * @param apiPermissionId API权限ID
     * @param request API权限保存请求
     * @param builtin 是否内置
     * @return API权限实体
     */
    private SysApiPermission buildApiPermission(Long apiPermissionId, ApiPermissionSaveRequest request, Boolean builtin) {
        return SysApiPermission.builder()
                .apiPermissionId(apiPermissionId)
                .appId(request.getAppId())
                .permissionCode(request.getPermissionCode())
                .permissionName(request.getPermissionName())
                .method(request.getMethod())
                .pathPattern(request.getPathPattern())
                .description(request.getDescription())
                .status(resolveStatus(request.getStatus()))
                .builtin(builtin)
                .sort(resolveSort(request.getSort()))
                .build();
    }

    /**
     * 根据请求构建系统应用实体。
     *
     * @param appId 应用ID
     * @param request 应用保存请求
     * @return 系统应用实体
     */
    private SysApp buildApp(Long appId, AppSaveRequest request) {
        return SysApp.builder()
                .appId(appId)
                .appCode(request.getAppCode())
                .appName(request.getAppName())
                .description(request.getDescription())
                .status(resolveStatus(request.getStatus()))
                .sort(resolveSort(request.getSort()))
                .build();
    }

    /**
     * 转换系统应用列表响应。
     *
     * @param app 系统应用实体
     * @return 系统应用列表响应
     */
    private SysAppVo toSysAppVo(SysApp app) {
        return SysAppVo.builder()
                .appId(app.getAppId())
                .appCode(app.getAppCode())
                .appName(app.getAppName())
                .description(app.getDescription())
                .status(app.getStatus())
                .sort(app.getSort())
                .permissionCount(countApiPermissionsByAppId(app.getAppId()))
                .build();
    }

    /**
     * 校验角色保存请求。
     *
     * @param request 角色保存请求
     * @param creating 是否创建场景
     */
    private void checkRoleRequest(RoleSaveRequest request, boolean creating) {
        if (request == null || !StringUtils.hasText(request.getRoleName())) {
            throw new AuthException("角色名称不能为空");
        }
        if (creating && !StringUtils.hasText(request.getRoleCode())) {
            throw new AuthException("角色编码不能为空");
        }
    }

    /**
     * 校验菜单保存请求。
     *
     * @param request 菜单保存请求
     */
    private void checkMenuRequest(MenuSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getMenuType()) || !StringUtils.hasText(request.getMenuName())) {
            throw new AuthException("菜单类型和名称不能为空");
        }
    }

    /**
     * 校验API权限保存请求。
     *
     * @param request API权限保存请求
     * @param creating 是否创建场景
     */
    private void checkApiPermissionRequest(ApiPermissionSaveRequest request, boolean creating) {
        if (request == null || !StringUtils.hasText(request.getPermissionName())
                || !StringUtils.hasText(request.getMethod()) || !StringUtils.hasText(request.getPathPattern())) {
            throw new AuthException("API权限名称、方法和路径不能为空");
        }
        if (request.getAppId() == null) {
            throw new AuthException("应用不能为空");
        }
        if (creating && !StringUtils.hasText(request.getPermissionCode())) {
            throw new AuthException("API权限编码不能为空");
        }
    }

    /**
     * 校验系统应用保存请求。
     *
     * @param request 应用保存请求
     * @param creating 是否创建场景
     */
    private void checkAppRequest(AppSaveRequest request, boolean creating) {
        if (request == null || !StringUtils.hasText(request.getAppName())) {
            throw new AuthException("应用名称不能为空");
        }
        if (creating && !StringUtils.hasText(request.getAppCode())) {
            throw new AuthException("应用编码不能为空");
        }
    }

    /**
     * 查询并要求应用启用。
     *
     * @param appId 应用ID
     * @return 应用实体
     */
    private SysApp requireEnabledApp(Long appId) {
        SysApp app = appMapper.selectById(appId);
        if (app == null || !UserStatusEnum.ENABLE.getCode().equals(app.getStatus())) {
            throw new AuthException("应用不存在或已禁用");
        }
        return app;
    }

    /**
     * 校验权限编码应用前缀。
     *
     * @param permissionCode 权限编码
     * @param app 应用实体
     */
    private void checkPermissionCodePrefix(String permissionCode, SysApp app) {
        if (!StringUtils.hasText(permissionCode) || !permissionCode.startsWith(app.getAppCode() + ":")) {
            throw new AuthException("权限编码必须以应用编码为前缀");
        }
    }

    /**
     * 判断HTTP方法是否匹配。
     *
     * @param permissionMethod 权限方法
     * @param requestMethod 请求方法
     * @return 是否匹配
     */
    private boolean matchesMethod(String permissionMethod, String requestMethod) {
        if (!StringUtils.hasText(permissionMethod)) {
            return false;
        }
        String normalizedPermissionMethod = permissionMethod.trim().toUpperCase(Locale.ROOT);
        return ALL_METHODS.equals(normalizedPermissionMethod) || normalizedPermissionMethod.equals(requestMethod);
    }

    /**
     * 判断应用内部路径是否匹配。
     *
     * @param pathPattern 路径匹配表达式
     * @param path 请求路径
     * @return 是否匹配
     */
    private boolean matchesPath(String pathPattern, String path) {
        return StringUtils.hasText(pathPattern) && PATH_MATCHER.match(pathPattern, path);
    }

    /**
     * 查询并要求角色存在。
     *
     * @param roleId 角色ID
     * @return 角色实体
     */
    private SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new AuthException("角色不存在");
        }
        return role;
    }

    /**
     * 根据角色编码查询角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    private SysRole selectRoleByCode(String roleCode) {
        return roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    /**
     * 查询并要求菜单存在。
     *
     * @param menuId 菜单ID
     * @return 菜单实体
     */
    private SysMenu requireMenu(Long menuId) {
        SysMenu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new AuthException("菜单不存在");
        }
        return menu;
    }

    /**
     * 查询并要求系统应用存在。
     *
     * @param appId 应用ID
     * @return 系统应用实体
     */
    private SysApp requireApp(Long appId) {
        SysApp app = appMapper.selectById(appId);
        if (app == null) {
            throw new AuthException("应用不存在");
        }
        return app;
    }

    /**
     * 根据应用编码查询系统应用。
     *
     * @param appCode 应用编码
     * @return 系统应用实体
     */
    private SysApp selectAppByCode(String appCode) {
        return appMapper.selectOne(Wrappers.<SysApp>lambdaQuery()
                .eq(SysApp::getAppCode, appCode)
                .last("LIMIT 1"));
    }

    /**
     * 统计应用下API权限数量。
     *
     * @param appId 应用ID
     * @return API权限数量
     */
    private Long countApiPermissionsByAppId(Long appId) {
        return apiPermissionMapper.selectCount(Wrappers.<SysApiPermission>lambdaQuery()
                .eq(SysApiPermission::getAppId, appId));
    }

    /**
     * 查询全部菜单。
     *
     * @return 菜单列表
     */
    private List<SysMenu> selectAllMenus() {
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSort, SysMenu::getMenuId));
    }

    /**
     * 查询并要求API权限存在。
     *
     * @param apiPermissionId API权限ID
     * @return API权限实体
     */
    private SysApiPermission requireApiPermission(Long apiPermissionId) {
        SysApiPermission permission = apiPermissionMapper.selectById(apiPermissionId);
        if (permission == null) {
            throw new AuthException("API权限不存在");
        }
        return permission;
    }

    /**
     * 校验用户存在。
     *
     * @param userId 用户ID
     */
    private void checkUserExists(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
    }

    /**
     * 校验不是内置数据。
     *
     * @param builtin 是否内置
     * @param message 错误提示
     */
    private void checkNotBuiltin(Boolean builtin, String message) {
        if (Boolean.TRUE.equals(builtin)) {
            throw new AuthException(message);
        }
    }

    /**
     * 解析状态默认值。
     *
     * @param status 状态
     * @return 状态
     */
    private Integer resolveStatus(Integer status) {
        return status == null ? UserStatusEnum.ENABLE.getCode() : status;
    }

    /**
     * 解析排序默认值。
     *
     * @param sort 排序值
     * @return 排序值
     */
    private Integer resolveSort(Integer sort) {
        return sort == null ? 0 : sort;
    }
}
