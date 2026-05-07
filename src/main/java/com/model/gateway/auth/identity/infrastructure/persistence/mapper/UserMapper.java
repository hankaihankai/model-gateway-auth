package com.model.gateway.auth.identity.infrastructure.persistence.mapper;

import com.model.gateway.auth.identity.domain.model.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
                phone,
                email,
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
                phone,
                email,
                status
            FROM sys_user
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    SysUser selectByUserId(Long userId);

    /**
     * 根据手机号查询用户。
     *
     * @param phone 手机号
     * @return 系统用户
     */
    @Select("""
            SELECT
                user_id AS userId,
                username,
                password,
                nickname,
                phone,
                email,
                status
            FROM sys_user
            WHERE phone = #{phone}
            LIMIT 1
            """)
    SysUser selectByPhone(String phone);

    /**
     * 根据邮箱查询用户。
     *
     * @param email 邮箱
     * @return 系统用户
     */
    @Select("""
            SELECT
                user_id AS userId,
                username,
                password,
                nickname,
                phone,
                email,
                status
            FROM sys_user
            WHERE email = #{email}
            LIMIT 1
            """)
    SysUser selectByEmail(String email);

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
                phone,
                email,
                status
            ) VALUES (
                #{username},
                #{password},
                #{nickname},
                #{phone},
                #{email},
                #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId", keyColumn = "user_id")
    int insert(SysUser user);

    /**
     * 更新用户状态。
     *
     * @param userId 用户ID
     * @param status 用户状态
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_user
            SET status = #{status}
            WHERE user_id = #{userId}
            """)
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 更新用户基本资料。
     *
     * @param user 用户基本资料
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_user
            SET nickname = #{nickname},
                phone = #{phone},
                email = #{email}
            WHERE user_id = #{userId}
            """)
    int updateProfile(SysUser user);

    /**
     * 更新用户密码。
     *
     * @param userId 用户ID
     * @param password BCrypt加密后的密码
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_user
            SET password = #{password}
            WHERE user_id = #{userId}
            """)
    int updatePassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 条件查询用户总数。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @return 用户总数
     */
    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user
            WHERE 1 = 1
            <if test="username != null and username != ''">
              AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="role != null and role != ''">
              AND EXISTS (
                SELECT 1
                FROM sys_user_role ur
                JOIN sys_role r ON r.role_id = ur.role_id
                WHERE ur.user_id = sys_user.user_id
                  AND r.role_code = #{role}
              )
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            </script>
            """)
    long selectCountByCondition(@Param("username") String username,
                                @Param("role") String role,
                                @Param("status") Integer status);

    /**
     * 条件查询用户分页列表。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 用户列表
     */
    @Select("""
            <script>
            SELECT
              user_id AS userId,
              username,
              password,
              nickname,
              phone,
              email,
              status
            FROM sys_user
            WHERE 1 = 1
            <if test="username != null and username != ''">
              AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="role != null and role != ''">
              AND EXISTS (
                SELECT 1
                FROM sys_user_role ur
                JOIN sys_role r ON r.role_id = ur.role_id
                WHERE ur.user_id = sys_user.user_id
                  AND r.role_code = #{role}
              )
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY user_id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<SysUser> selectListByCondition(@Param("username") String username,
                                       @Param("role") String role,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);
}
