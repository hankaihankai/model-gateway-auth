package com.model.gateway.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.mapper.UserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token角色加载配置。
 */
@Component
public class SaTokenRoleConfig implements StpInterface {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * 创建Sa-Token角色加载配置。
     *
     * @param userMapper 用户数据访问对象
     */
    public SaTokenRoleConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 获取当前账号角色列表。
     *
     * @param loginId 登录账号ID
     * @param loginType 登录账号类型
     * @return 角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userMapper.selectByUserId(Long.valueOf(String.valueOf(loginId)));
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }

    /**
     * 获取当前账号权限码列表。
     *
     * @param loginId 登录账号ID
     * @param loginType 登录账号类型
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
