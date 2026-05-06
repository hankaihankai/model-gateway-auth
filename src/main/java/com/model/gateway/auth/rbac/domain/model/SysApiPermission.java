package com.model.gateway.auth.rbac.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API权限实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysApiPermission {

    /**
     * API权限ID。
     */
    private Long apiPermissionId;

    /**
     * 权限编码。
     */
    private String permissionCode;

    /**
     * 权限名称。
     */
    private String permissionName;

    /**
     * HTTP方法。
     */
    private String method;

    /**
     * 路径匹配表达式。
     */
    private String pathPattern;

    /**
     * 权限说明。
     */
    private String description;

    /**
     * 权限状态：0启用、1禁用。
     */
    private Integer status;

    /**
     * 是否内置权限。
     */
    private Boolean builtin;

    /**
     * 排序值。
     */
    private Integer sort;
}
