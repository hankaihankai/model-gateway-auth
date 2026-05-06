package com.model.gateway.auth.rbac.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色授权请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleGrantRequest {

    /**
     * 菜单ID列表。
     */
    private List<Long> menuIds;

    /**
     * API权限ID列表。
     */
    private List<Long> apiPermissionIds;
}
