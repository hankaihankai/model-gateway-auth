package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色数据访问对象。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询启用角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 0
            ORDER BY r.sort ASC, r.role_id ASC
            """)
    List<String> selectRoleCodesByUserId(Long userId);
}
