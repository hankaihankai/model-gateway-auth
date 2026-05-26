package com.model.gateway.auth.system.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统应用列表响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysAppVo {

    /**
     * 应用ID。
     */
    private Long appId;

    /**
     * 应用编码。
     */
    private String appCode;

    /**
     * 应用名称。
     */
    private String appName;

    /**
     * 应用说明。
     */
    private String description;

    /**
     * 应用状态：0启用、1禁用。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sort;

    /**
     * API权限数量。
     */
    private Long permissionCount;
}
