package com.model.gateway.auth.system.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API权限保存请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiPermissionSaveRequest {

    /**
     * 应用ID。
     */
    private Long appId;

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
     * 排序值。
     */
    private Integer sort;
}
