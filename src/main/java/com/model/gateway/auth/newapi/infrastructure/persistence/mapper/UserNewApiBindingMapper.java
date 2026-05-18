package com.model.gateway.auth.newapi.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.model.gateway.auth.newapi.domain.model.UserNewApiBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * new-api绑定数据访问对象。
 */
@Mapper
public interface UserNewApiBindingMapper extends BaseMapper<UserNewApiBinding> {
}
