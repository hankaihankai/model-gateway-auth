package com.model.gateway.auth.rbac.infrastructure.persistence.mapper;

import com.model.gateway.auth.rbac.domain.model.SysApiPermission;
import com.model.gateway.auth.rbac.domain.model.SysMenu;
import com.model.gateway.auth.rbac.domain.model.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * RBAC数据访问对象。
 */
@Mapper
public interface RbacMapper {

    /**
     * 查询启用角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 0
            ORDER BY r.sort ASC, r.role_id ASC
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 查询用户权限编码列表。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    @Select("""
            SELECT DISTINCT permission_code
            FROM (
              SELECT m.permission_code
              FROM sys_user_role ur
              JOIN sys_role r ON r.role_id = ur.role_id AND r.status = 0
              JOIN sys_role_menu rm ON rm.role_id = r.role_id
              JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.status = 0
              WHERE ur.user_id = #{userId}
                AND m.permission_code IS NOT NULL
                AND m.permission_code <> ''
              UNION
              SELECT ap.permission_code
              FROM sys_user_role ur
              JOIN sys_role r ON r.role_id = ur.role_id AND r.status = 0
              JOIN sys_role_api_permission rap ON rap.role_id = r.role_id
              JOIN sys_api_permission ap ON ap.api_permission_id = rap.api_permission_id AND ap.status = 0
              WHERE ur.user_id = #{userId}
                AND ap.permission_code IS NOT NULL
                AND ap.permission_code <> ''
            ) p
            ORDER BY permission_code ASC
            """)
    List<String> selectPermissionCodesByUserId(Long userId);

    /**
     * 查询用户可见菜单和按钮。
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Select("""
            SELECT DISTINCT
              m.menu_id AS menuId,
              m.parent_id AS parentId,
              m.menu_type AS menuType,
              m.menu_name AS menuName,
              m.path,
              m.component_key AS componentKey,
              m.permission_code AS permissionCode,
              m.icon,
              m.visible,
              m.status,
              m.builtin,
              m.sort
            FROM sys_user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id AND r.status = 0
            JOIN sys_role_menu rm ON rm.role_id = r.role_id
            JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.status = 0
            WHERE ur.user_id = #{userId}
            ORDER BY m.sort ASC, m.menu_id ASC
            """)
    List<SysMenu> selectMenusByUserId(Long userId);

    /**
     * 查询所有角色。
     *
     * @return 角色列表
     */
    @Select("""
            SELECT
              role_id AS roleId,
              role_code AS roleCode,
              role_name AS roleName,
              description,
              status,
              builtin,
              sort
            FROM sys_role
            ORDER BY sort ASC, role_id ASC
            """)
    List<SysRole> selectAllRoles();

    /**
     * 根据角色ID查询角色。
     *
     * @param roleId 角色ID
     * @return 角色实体
     */
    @Select("""
            SELECT
              role_id AS roleId,
              role_code AS roleCode,
              role_name AS roleName,
              description,
              status,
              builtin,
              sort
            FROM sys_role
            WHERE role_id = #{roleId}
            LIMIT 1
            """)
    SysRole selectRoleById(Long roleId);

    /**
     * 根据角色编码查询角色。
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    @Select("""
            SELECT
              role_id AS roleId,
              role_code AS roleCode,
              role_name AS roleName,
              description,
              status,
              builtin,
              sort
            FROM sys_role
            WHERE role_code = #{roleCode}
            LIMIT 1
            """)
    SysRole selectRoleByCode(String roleCode);

    /**
     * 新增角色。
     *
     * @param role 角色实体
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO sys_role (
              role_code,
              role_name,
              description,
              status,
              builtin,
              sort
            ) VALUES (
              #{roleCode},
              #{roleName},
              #{description},
              #{status},
              #{builtin},
              #{sort}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "roleId", keyColumn = "role_id")
    int insertRole(SysRole role);

    /**
     * 更新角色。
     *
     * @param role 角色实体
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_role
            SET role_name = #{roleName},
                description = #{description},
                status = #{status},
                sort = #{sort}
            WHERE role_id = #{roleId}
            """)
    int updateRole(SysRole role);

    /**
     * 删除角色。
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role WHERE role_id = #{roleId} AND builtin = 0")
    int deleteRole(Long roleId);

    /**
     * 查询所有菜单。
     *
     * @return 菜单列表
     */
    @Select("""
            SELECT
              menu_id AS menuId,
              parent_id AS parentId,
              menu_type AS menuType,
              menu_name AS menuName,
              path,
              component_key AS componentKey,
              permission_code AS permissionCode,
              icon,
              visible,
              status,
              builtin,
              sort
            FROM sys_menu
            ORDER BY sort ASC, menu_id ASC
            """)
    List<SysMenu> selectAllMenus();

    /**
     * 根据菜单ID查询菜单。
     *
     * @param menuId 菜单ID
     * @return 菜单实体
     */
    @Select("""
            SELECT
              menu_id AS menuId,
              parent_id AS parentId,
              menu_type AS menuType,
              menu_name AS menuName,
              path,
              component_key AS componentKey,
              permission_code AS permissionCode,
              icon,
              visible,
              status,
              builtin,
              sort
            FROM sys_menu
            WHERE menu_id = #{menuId}
            LIMIT 1
            """)
    SysMenu selectMenuById(Long menuId);

    /**
     * 新增菜单。
     *
     * @param menu 菜单实体
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO sys_menu (
              parent_id,
              menu_type,
              menu_name,
              path,
              component_key,
              permission_code,
              icon,
              visible,
              status,
              builtin,
              sort
            ) VALUES (
              #{parentId},
              #{menuType},
              #{menuName},
              #{path},
              #{componentKey},
              #{permissionCode},
              #{icon},
              #{visible},
              #{status},
              #{builtin},
              #{sort}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "menuId", keyColumn = "menu_id")
    int insertMenu(SysMenu menu);

    /**
     * 更新菜单。
     *
     * @param menu 菜单实体
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_menu
            SET parent_id = #{parentId},
                menu_type = #{menuType},
                menu_name = #{menuName},
                path = #{path},
                component_key = #{componentKey},
                permission_code = #{permissionCode},
                icon = #{icon},
                visible = #{visible},
                status = #{status},
                sort = #{sort}
            WHERE menu_id = #{menuId}
            """)
    int updateMenu(SysMenu menu);

    /**
     * 删除菜单。
     *
     * @param menuId 菜单ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_menu WHERE menu_id = #{menuId} AND builtin = 0")
    int deleteMenu(Long menuId);

    /**
     * 查询菜单子节点数量。
     *
     * @param menuId 菜单ID
     * @return 子节点数量
     */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{menuId}")
    long countMenuChildren(Long menuId);

    /**
     * 查询所有API权限。
     *
     * @return API权限列表
     */
    @Select("""
            SELECT
              api_permission_id AS apiPermissionId,
              permission_code AS permissionCode,
              permission_name AS permissionName,
              method,
              path_pattern AS pathPattern,
              description,
              status,
              builtin,
              sort
            FROM sys_api_permission
            ORDER BY sort ASC, api_permission_id ASC
            """)
    List<SysApiPermission> selectAllApiPermissions();

    /**
     * 根据API权限ID查询权限。
     *
     * @param apiPermissionId API权限ID
     * @return API权限实体
     */
    @Select("""
            SELECT
              api_permission_id AS apiPermissionId,
              permission_code AS permissionCode,
              permission_name AS permissionName,
              method,
              path_pattern AS pathPattern,
              description,
              status,
              builtin,
              sort
            FROM sys_api_permission
            WHERE api_permission_id = #{apiPermissionId}
            LIMIT 1
            """)
    SysApiPermission selectApiPermissionById(Long apiPermissionId);

    /**
     * 新增API权限。
     *
     * @param permission API权限实体
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO sys_api_permission (
              permission_code,
              permission_name,
              method,
              path_pattern,
              description,
              status,
              builtin,
              sort
            ) VALUES (
              #{permissionCode},
              #{permissionName},
              #{method},
              #{pathPattern},
              #{description},
              #{status},
              #{builtin},
              #{sort}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "apiPermissionId", keyColumn = "api_permission_id")
    int insertApiPermission(SysApiPermission permission);

    /**
     * 更新API权限。
     *
     * @param permission API权限实体
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_api_permission
            SET permission_name = #{permissionName},
                method = #{method},
                path_pattern = #{pathPattern},
                description = #{description},
                status = #{status},
                sort = #{sort}
            WHERE api_permission_id = #{apiPermissionId}
            """)
    int updateApiPermission(SysApiPermission permission);

    /**
     * 删除API权限。
     *
     * @param apiPermissionId API权限ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_api_permission WHERE api_permission_id = #{apiPermissionId} AND builtin = 0")
    int deleteApiPermission(Long apiPermissionId);

    /**
     * 查询角色菜单ID。
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 查询角色API权限ID。
     *
     * @param roleId 角色ID
     * @return API权限ID列表
     */
    @Select("SELECT api_permission_id FROM sys_role_api_permission WHERE role_id = #{roleId}")
    List<Long> selectApiPermissionIdsByRoleId(Long roleId);

    /**
     * 删除角色菜单授权。
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(Long roleId);

    /**
     * 新增角色菜单授权。
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /**
     * 删除角色API权限授权。
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_api_permission WHERE role_id = #{roleId}")
    int deleteRoleApiPermissions(Long roleId);

    /**
     * 新增角色API权限授权。
     *
     * @param roleId 角色ID
     * @param apiPermissionId API权限ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_role_api_permission (role_id, api_permission_id) VALUES (#{roleId}, #{apiPermissionId})")
    int insertRoleApiPermission(@Param("roleId") Long roleId, @Param("apiPermissionId") Long apiPermissionId);

    /**
     * 查询用户角色ID。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 删除用户角色关联。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(Long userId);

    /**
     * 新增用户角色关联。
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
