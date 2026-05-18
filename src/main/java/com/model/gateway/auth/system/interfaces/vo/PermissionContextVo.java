package com.model.gateway.auth.system.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户权限上下文响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionContextVo {

    /**
     * 角色编码列表。
     */
    private List<String> roles;

    /**
     * 权限编码列表。
     */
    private List<String> permissions;

    /**
     * 动态菜单树。
     */
    private List<MenuTreeVo> menus;
}
