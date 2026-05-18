package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysRoleApiPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色API权限关联数据访问对象。
 */
@Mapper
public interface SysRoleApiPermissionMapper extends BaseMapper<SysRoleApiPermission> {
}
