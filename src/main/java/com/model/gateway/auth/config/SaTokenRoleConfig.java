package com.model.gateway.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.model.gateway.auth.system.application.RbacApplicationService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token角色加载配置。
 */
@Component
public class SaTokenRoleConfig implements StpInterface {

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * 创建Sa-Token角色加载配置。
     *
     * @param rbacApplicationService RBAC应用服务
     */
    public SaTokenRoleConfig(RbacApplicationService rbacApplicationService) {
        this.rbacApplicationService = rbacApplicationService;
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
        return rbacApplicationService.getRoleCodes(Long.valueOf(String.valueOf(loginId)));
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
        return rbacApplicationService.getPermissionCodes(Long.valueOf(String.valueOf(loginId)));
    }
}
