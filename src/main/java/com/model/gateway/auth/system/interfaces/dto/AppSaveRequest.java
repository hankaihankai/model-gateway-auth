package com.model.gateway.auth.system.interfaces.dto;

import lombok.Data;

/**
 * 系统应用保存请求。
 */
@Data
public class AppSaveRequest {

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
}
