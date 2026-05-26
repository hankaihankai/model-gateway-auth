package com.model.gateway.auth.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.system.domain.model.SysApp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统应用数据访问对象。
 */
@Mapper
public interface SysAppMapper extends BaseMapper<SysApp> {
}
