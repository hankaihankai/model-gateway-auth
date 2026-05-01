package com.model.gateway.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * new-api绑定操作日志实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserNewApiBindingLog {

    /**
     * 日志ID。
     */
    private Long id;

    /**
     * 业务用户ID。
     */
    private Long userId;

    /**
     * 绑定ID。
     */
    private Long bindingId;

    /**
     * 操作类型。
     */
    private String operateType;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 操作说明。
     */
    private String message;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
