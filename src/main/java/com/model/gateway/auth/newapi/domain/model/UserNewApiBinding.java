package com.model.gateway.auth.newapi.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 业务用户与new-api用户绑定实体。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_new_api_binding")
public class UserNewApiBinding {

    /**
     * 绑定ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务系统用户ID。
     */
    private Long userId;

    /**
     * new-api用户ID。
     */
    private Long newApiUserId;

    /**
     * new-api用户名。
     */
    private String newApiUserName;

    /**
     * 完整new-api API Key。
     */
    private String newApiApiKey;

    /**
     * 绑定状态。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
