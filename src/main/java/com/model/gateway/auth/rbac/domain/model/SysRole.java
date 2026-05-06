package com.model.gateway.auth.rbac.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统角色实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysRole {

    /**
     * 角色ID。
     */
    private Long roleId;

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
     * 是否内置角色。
     */
    private Boolean builtin;

    /**
     * 排序值。
     */
    private Integer sort;
}
