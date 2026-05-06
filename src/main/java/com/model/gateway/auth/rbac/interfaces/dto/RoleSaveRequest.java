package com.model.gateway.auth.rbac.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色保存请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleSaveRequest {

    /**
     * 角色编码。
     */
    private String roleCode;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 角色说明。
     */
    private String description;

    /**
     * 角色状态：0启用、1禁用。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sort;
}
