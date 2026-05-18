package com.model.gateway.auth.newapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.newapi.domain.model.UserNewApiBindingLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * new-api绑定操作日志数据访问对象。
 */
@Mapper
public interface UserNewApiBindingLogMapper extends BaseMapper<UserNewApiBindingLog> {
}
