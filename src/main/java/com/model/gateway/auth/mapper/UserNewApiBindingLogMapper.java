package com.model.gateway.auth.mapper;

import com.model.gateway.auth.domain.UserNewApiBindingLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * new-api绑定操作日志数据访问对象。
 */
@Mapper
public interface UserNewApiBindingLogMapper {

    /**
     * 新增new-api绑定操作日志。
     *
     * @param log 绑定操作日志
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO user_new_api_binding_log (
                user_id,
                binding_id,
                operate_type,
                success,
                message
            ) VALUES (
                #{userId},
                #{bindingId},
                #{operateType},
                #{success},
                #{message}
            )
            """)
    int insert(UserNewApiBindingLog log);
}
