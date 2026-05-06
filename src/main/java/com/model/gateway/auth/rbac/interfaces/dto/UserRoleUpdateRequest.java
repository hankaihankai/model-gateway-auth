package com.model.gateway.auth.rbac.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户角色更新请求。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleUpdateRequest {

    /**
     * 角色ID列表。
     */
    private List<Long> roleIds;
}
