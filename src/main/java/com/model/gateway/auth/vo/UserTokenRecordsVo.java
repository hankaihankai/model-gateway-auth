package com.model.gateway.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户Token使用记录分页响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenRecordsVo {

    /**
     * 页码。
     */
    private Integer page;

    /**
     * 每页数量。
     */
    private Integer pageSize;

    /**
     * 总数量。
     */
    private Long total;

    /**
     * 使用记录列表。
     */
    private List<UserTokenRecordItemVo> items;

    /**
     * 用户Token使用记录项。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserTokenRecordItemVo {

        /**
         * 记录ID。
         */
        private Long id;

        /**
         * new-api用户ID。
         */
        private Long newApiUserId;

        /**
         * new-api用户名。
         */
        private String newApiUserName;

        /**
         * 模型名称。
         */
        private String modelName;

        /**
         * 记录创建Unix时间戳秒。
         */
        private Long createdAt;

        /**
         * 消耗Token数。
         */
        private Long tokenUsed;

        /**
         * 请求次数。
         */
        private Long count;

        /**
         * 消耗额度。
         */
        private Long quota;
    }
}
