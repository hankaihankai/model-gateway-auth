package com.model.gateway.auth.mapper;

import com.model.gateway.auth.domain.UserNewApiBinding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * new-api绑定数据访问对象。
 */
@Mapper
public interface UserNewApiBindingMapper {

    /**
     * 根据业务用户ID查询new-api绑定。
     *
     * @param userId 业务用户ID
     * @return new-api绑定
     */
    @Select("""
            SELECT
                id,
                user_id AS userId,
                new_api_user_id AS newApiUserId,
                new_api_user_name AS newApiUserName,
                new_api_api_key AS newApiApiKey,
                status,
                create_time AS createTime,
                update_time AS updateTime
            FROM user_new_api_binding
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    UserNewApiBinding selectByUserId(Long userId);

    /**
     * 新增new-api绑定。
     *
     * @param binding new-api绑定
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO user_new_api_binding (
                user_id,
                new_api_user_id,
                new_api_user_name,
                new_api_api_key,
                status
            ) VALUES (
                #{userId},
                #{newApiUserId},
                #{newApiUserName},
                #{newApiApiKey},
                #{status}
            )
            """)
    int insert(UserNewApiBinding binding);
}
