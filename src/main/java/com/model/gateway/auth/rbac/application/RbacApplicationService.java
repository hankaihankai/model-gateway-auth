package com.model.gateway.auth.rbac.application;

import com.model.gateway.auth.identity.domain.model.SysUser;
import com.model.gateway.auth.identity.infrastructure.persistence.mapper.UserMapper;
import com.model.gateway.auth.rbac.domain.model.SysApiPermission;
import com.model.gateway.auth.rbac.domain.model.SysMenu;
import com.model.gateway.auth.rbac.domain.model.SysRole;
import com.model.gateway.auth.rbac.infrastructure.persistence.mapper.RbacMapper;
import com.model.gateway.auth.rbac.interfaces.dto.ApiPermissionSaveRequest;
import com.model.gateway.auth.rbac.interfaces.dto.MenuSaveRequest;
import com.model.gateway.auth.rbac.interfaces.dto.RoleGrantRequest;
import com.model.gateway.auth.rbac.interfaces.dto.RoleSaveRequest;
import com.model.gateway.auth.rbac.interfaces.dto.UserRoleUpdateRequest;
import com.model.gateway.auth.rbac.interfaces.vo.MenuTreeVo;
import com.model.gateway.auth.rbac.interfaces.vo.PermissionContextVo;
import com.model.gateway.auth.rbac.interfaces.vo.RoleGrantVo;
import com.model.gateway.auth.shared.enums.UserStatusEnum;
import com.model.gateway.auth.shared.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
     * RBAC数据访问对象。
     */
    private final RbacMapper rbacMapper;

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建RBAC应用服务。
     *
     * @param rbacMapper RBAC数据访问对象
     * @param userMapper 用户数据访问对象
     * @param transactionTemplate 事务模板
     */
    public RbacApplicationService(
            RbacMapper rbacMapper,
            UserMapper userMapper,
            TransactionTemplate transactionTemplate) {
        this.rbacMapper = rbacMapper;
        this.userMapper = userMapper;
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
                .menus(buildMenuTree(rbacMapper.selectMenusByUserId(userId), true))
                .build();
    }

    /**
     * 查询用户角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    public List<String> getRoleCodes(Long userId) {
        return rbacMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 查询用户权限编码列表。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    public List<String> getPermissionCodes(Long userId) {
        return rbacMapper.selectPermissionCodesByUserId(userId);
    }

    /**
     * 为用户绑定默认普通用户角色。
     *
     * @param userId 用户ID
     */
    public void assignDefaultUserRole(Long userId) {
        SysRole role = rbacMapper.selectRoleByCode(DEFAULT_USER_ROLE_CODE);
        if (role != null) {
            rbacMapper.insertUserRole(userId, role.getRoleId());
        }
    }

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    public List<SysRole> listRoles() {
        return rbacMapper.selectAllRoles();
    }

    /**
     * 创建角色。
     *
     * @param request 角色保存请求
     * @return 角色实体
     */
    public SysRole createRole(RoleSaveRequest request) {
        checkRoleRequest(request, true);
        if (rbacMapper.selectRoleByCode(request.getRoleCode()) != null) {
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
        rbacMapper.insertRole(role);
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
        rbacMapper.updateRole(exists);
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
        int rows = rbacMapper.deleteRole(roleId);
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
                .menuIds(rbacMapper.selectMenuIdsByRoleId(roleId))
                .apiPermissionIds(rbacMapper.selectApiPermissionIdsByRoleId(roleId))
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
            rbacMapper.deleteRoleMenus(roleId);
            for (Long menuId : menuIds) {
                rbacMapper.insertRoleMenu(roleId, menuId);
            }
            rbacMapper.deleteRoleApiPermissions(roleId);
            for (Long apiPermissionId : apiPermissionIds) {
                rbacMapper.insertRoleApiPermission(roleId, apiPermissionId);
            }
        });
    }

    /**
     * 查询菜单树。
     *
     * @return 菜单树
     */
    public List<MenuTreeVo> listMenuTree() {
        return buildMenuTree(rbacMapper.selectAllMenus(), false);
    }

    /**
     * 查询菜单平铺列表。
     *
     * @return 菜单列表
     */
    public List<SysMenu> listMenus() {
        return rbacMapper.selectAllMenus();
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
        rbacMapper.insertMenu(menu);
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
        rbacMapper.updateMenu(menu);
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
        if (rbacMapper.countMenuChildren(menuId) > 0) {
            throw new AuthException("存在子菜单，不能删除");
        }
        int rows = rbacMapper.deleteMenu(menuId);
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
        return rbacMapper.selectAllApiPermissions();
    }

    /**
     * 创建API权限。
     *
     * @param request API权限保存请求
     * @return API权限实体
     */
    public SysApiPermission createApiPermission(ApiPermissionSaveRequest request) {
        checkApiPermissionRequest(request, true);
        SysApiPermission permission = buildApiPermission(null, request, Boolean.FALSE);
        rbacMapper.insertApiPermission(permission);
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
        SysApiPermission permission = buildApiPermission(apiPermissionId, request, exists.getBuiltin());
        rbacMapper.updateApiPermission(permission);
        return permission;
    }

    /**
     * 删除API权限。
     *
     * @param apiPermissionId API权限ID
     */
    public void deleteApiPermission(Long apiPermissionId) {
        SysApiPermission permission = requireApiPermission(apiPermissionId);
        checkNotBuiltin(permission.getBuiltin(), "内置API权限不能删除");
        int rows = rbacMapper.deleteApiPermission(apiPermissionId);
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
        return rbacMapper.selectRoleIdsByUserId(userId);
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
            rbacMapper.deleteUserRoles(userId);
            for (Long roleId : roleIds) {
                requireRole(roleId);
                rbacMapper.insertUserRole(userId, roleId);
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
        if (creating && !StringUtils.hasText(request.getPermissionCode())) {
            throw new AuthException("API权限编码不能为空");
        }
    }

    /**
     * 查询并要求角色存在。
     *
     * @param roleId 角色ID
     * @return 角色实体
     */
    private SysRole requireRole(Long roleId) {
        SysRole role = rbacMapper.selectRoleById(roleId);
        if (role == null) {
            throw new AuthException("角色不存在");
        }
        return role;
    }

    /**
     * 查询并要求菜单存在。
     *
     * @param menuId 菜单ID
     * @return 菜单实体
     */
    private SysMenu requireMenu(Long menuId) {
        SysMenu menu = rbacMapper.selectMenuById(menuId);
        if (menu == null) {
            throw new AuthException("菜单不存在");
        }
        return menu;
    }

    /**
     * 查询并要求API权限存在。
     *
     * @param apiPermissionId API权限ID
     * @return API权限实体
     */
    private SysApiPermission requireApiPermission(Long apiPermissionId) {
        SysApiPermission permission = rbacMapper.selectApiPermissionById(apiPermissionId);
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
        SysUser user = userMapper.selectByUserId(userId);
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
