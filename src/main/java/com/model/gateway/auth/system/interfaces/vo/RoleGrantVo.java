package com.model.gateway.auth.system.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色授权响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleGrantVo {

    /**
     * 菜单ID列表。
     */
    private List<Long> menuIds;

    /**
     * API权限ID列表。
     */
    private List<Long> apiPermissionIds;
}
