package com.model.gateway.auth.system.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统应用实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_app")
public class SysApp {

    /**
     * 应用ID。
     */
    @TableId(value = "app_id", type = IdType.AUTO)
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
}
