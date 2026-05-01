package com.model.gateway.auth.mapper;

import com.model.gateway.auth.domain.UserNewApiBinding;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserNewApiBinding binding);

    /**
     * 更新new-api绑定为启用状态。
     *
     * @param binding new-api绑定
     * @return 影响行数
     */
    @Update("""
            UPDATE user_new_api_binding
            SET
                new_api_user_id = #{newApiUserId},
                new_api_user_name = #{newApiUserName},
                new_api_api_key = #{newApiApiKey},
                status = #{status}
            WHERE id = #{id}
            """)
    int updateBinding(UserNewApiBinding binding);

    /**
     * 更新new-api绑定状态。
     *
     * @param id 绑定ID
     * @param status 绑定状态
     * @return 影响行数
     */
    @Update("""
            UPDATE user_new_api_binding
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
