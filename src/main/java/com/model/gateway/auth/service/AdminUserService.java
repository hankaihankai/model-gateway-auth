package com.model.gateway.auth.service;

import com.model.gateway.auth.common.AuthConstants;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.domain.UserNewApiBinding;
import com.model.gateway.auth.dto.AdminUserCreateRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.mapper.UserNewApiBindingMapper;
import com.model.gateway.auth.vo.AdminUserCreateResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理员用户管理业务服务。
 */
@Service
public class AdminUserService {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * new-api绑定数据访问对象。
     */
    private final UserNewApiBindingMapper bindingMapper;

    /**
     * BCrypt密码编码器。
     */
    private final BCryptPasswordEncoder passwordEncoder;
    /**
     * 创建管理员用户管理业务服务。
     *
     * @param userMapper 用户数据访问对象
     * @param bindingMapper new-api绑定数据访问对象
     * @param passwordEncoder BCrypt密码编码器
     */
    public AdminUserService(UserMapper userMapper, UserNewApiBindingMapper bindingMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建系统用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminUserCreateResponse createUser(AdminUserCreateRequest request) {
        checkCreateRequest(request);
        SysUser exists = userMapper.selectByUsername(request.getUsername());
        if (exists != null) {
            throw new AuthException("用户名已存在");
        }

        SysUser user = SysUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(resolveRole(request.getRole()))
                .status(resolveStatus(request.getStatus()))
                .build();
        userMapper.insert(user);

        boolean newApiBound = createBindingIfPresent(user.getUserId(), request);
        return AdminUserCreateResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .newApiBound(newApiBound)
                .build();
    }

    /**
     * 校验创建用户请求。
     *
     * @param request 创建用户请求
     */
    private void checkCreateRequest(AdminUserCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new AuthException("用户名和密码不能为空");
        }
        boolean hasAnyNewApi = request.getNewApiUserId() != null
                || StringUtils.hasText(request.getNewApiUserName())
                || StringUtils.hasText(request.getNewApiApiKey());
        boolean hasAllNewApi = request.getNewApiUserId() != null
                && StringUtils.hasText(request.getNewApiUserName())
                && StringUtils.hasText(request.getNewApiApiKey());
        if (hasAnyNewApi && !hasAllNewApi) {
            throw new AuthException("new-api绑定参数不完整");
        }
    }

    /**
     * 创建可选new-api绑定。
     *
     * @param userId 用户ID
     * @param request 创建用户请求
     * @return 是否创建绑定
     */
    private boolean createBindingIfPresent(Long userId, AdminUserCreateRequest request) {
        if (request.getNewApiUserId() == null
                && !StringUtils.hasText(request.getNewApiUserName())
                && !StringUtils.hasText(request.getNewApiApiKey())) {
            return false;
        }
        bindingMapper.insert(UserNewApiBinding.builder()
                .userId(userId)
                .newApiUserId(request.getNewApiUserId())
                .newApiUserName(request.getNewApiUserName())
                .newApiApiKey(request.getNewApiApiKey())
                .status(UserStatusEnum.ENABLE.getCode())
                .build());
        return true;
    }

    /**
     * 解析用户角色。
     *
     * @param role 用户角色
     * @return 用户角色
     */
    private String resolveRole(String role) {
        if (StringUtils.hasText(role)) {
            return role;
        }
        return AuthConstants.ROLE_USER;
    }

    /**
     * 解析用户状态。
     *
     * @param status 用户状态
     * @return 用户状态
     */
    private String resolveStatus(String status) {
        if (StringUtils.hasText(status)) {
            return status;
        }
        return UserStatusEnum.ENABLE.getCode();
    }
}
