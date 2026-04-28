package com.model.gateway.auth.mapper;

import com.model.gateway.auth.domain.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问对象。
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 系统用户
     */
    @Select("""
            SELECT
                user_id AS userId,
                username,
                password,
                nickname,
                role,
                status
            FROM sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    SysUser selectByUsername(String username);

    /**
     * 根据用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 系统用户
     */
    @Select("""
            SELECT
                user_id AS userId,
                username,
                password,
                nickname,
                role,
                status
            FROM sys_user
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    SysUser selectByUserId(Long userId);

    /**
     * 新增系统用户。
     *
     * @param user 系统用户
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO sys_user (
                username,
                password,
                nickname,
                role,
                status
            ) VALUES (
                #{username},
                #{password},
                #{nickname},
                #{role},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    int insert(SysUser user);
}
