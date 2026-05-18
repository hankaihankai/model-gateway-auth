package com.model.gateway.auth.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统菜单实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_menu")
public class SysMenu {

    /**
     * 菜单ID。
     */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /**
     * 父级菜单ID。
     */
    private Long parentId;

    /**
     * 菜单类型：DIR、MENU、BUTTON。
     */
    private String menuType;

    /**
     * 菜单名称。
     */
    private String menuName;

    /**
     * 前端路由路径。
     */
    private String path;

    /**
     * 前端组件白名单Key。
     */
    private String componentKey;

    /**
     * 权限编码。
     */
    private String permissionCode;

    /**
     * 菜单图标。
     */
    private String icon;

    /**
     * 是否可见。
     */
    private Boolean visible;

    /**
     * 菜单状态：0启用、1禁用。
     */
    private Integer status;

    /**
     * 是否内置菜单。
     */
    private Boolean builtin;

    /**
     * 排序值。
     */
    private Integer sort;
}
