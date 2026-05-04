package com.model.gateway.auth.identity.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员用户列表分页响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserListPageVo {

    /**
     * 用户列表。
     */
    private List<AdminUserListItemVo> list;

    /**
     * 总数。
     */
    private long total;

    /**
     * 页码。
     */
    private int pageNo;

    /**
     * 每页数量。
     */
    private int pageSize;
}
