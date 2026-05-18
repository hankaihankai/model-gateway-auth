package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联数据访问对象。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
