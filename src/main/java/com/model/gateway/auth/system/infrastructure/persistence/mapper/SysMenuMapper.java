package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统菜单数据访问对象。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

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
}
