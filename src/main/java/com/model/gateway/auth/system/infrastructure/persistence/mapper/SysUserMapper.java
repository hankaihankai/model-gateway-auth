package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户数据访问对象。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

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
