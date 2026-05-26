package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysApiPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统API权限数据访问对象。
 */
@Mapper
public interface SysApiPermissionMapper extends BaseMapper<SysApiPermission> {

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
     * 按应用排序查询API权限列表。
     *
     * @return API权限列表
     */
    @Select("""
            SELECT
              ap.api_permission_id AS apiPermissionId,
              ap.app_id AS appId,
              ap.permission_code AS permissionCode,
              ap.permission_name AS permissionName,
              ap.method,
              ap.path_pattern AS pathPattern,
              ap.description,
              ap.status,
              ap.builtin,
              ap.sort
            FROM sys_api_permission ap
            JOIN sys_app a ON a.app_id = ap.app_id
            ORDER BY a.sort ASC, ap.sort ASC, ap.api_permission_id ASC
            """)
    List<SysApiPermission> selectAllByAppOrder();

    /**
     * 查询用户在指定应用下启用的API权限。
     *
     * @param userId 用户ID
     * @param appCode 应用编码
     * @return API权限列表
     */
    @Select("""
            SELECT DISTINCT
              ap.api_permission_id AS apiPermissionId,
              ap.app_id AS appId,
              ap.permission_code AS permissionCode,
              ap.permission_name AS permissionName,
              ap.method,
              ap.path_pattern AS pathPattern,
              ap.description,
              ap.status,
              ap.builtin,
              ap.sort
            FROM sys_user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id AND r.status = 0
            JOIN sys_role_api_permission rap ON rap.role_id = r.role_id
            JOIN sys_api_permission ap ON ap.api_permission_id = rap.api_permission_id AND ap.status = 0
            JOIN sys_app a ON a.app_id = ap.app_id AND a.status = 0
            WHERE ur.user_id = #{userId}
              AND a.app_code = #{appCode}
            ORDER BY ap.sort ASC, ap.api_permission_id ASC
            """)
    List<SysApiPermission> selectEnabledByUserIdAndAppCode(@Param("userId") Long userId,
                                                           @Param("appCode") String appCode);
}
