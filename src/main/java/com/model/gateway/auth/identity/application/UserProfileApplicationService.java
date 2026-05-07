package com.model.gateway.auth.identity.application;

import cn.dev33.satoken.stp.StpUtil;
import com.model.gateway.auth.newapi.application.NewApiBindingApplicationService;
import com.model.gateway.auth.newapi.infrastructure.config.NewApiUserManagerProperties;
import com.model.gateway.auth.newapi.infrastructure.external.NewApiUserAcl;
import com.model.gateway.auth.identity.infrastructure.cache.GatewayCredentialCacheService;
import com.model.gateway.auth.rbac.application.RbacApplicationService;
import com.model.gateway.auth.shared.enums.UserStatusEnum;
import com.model.gateway.auth.config.SaTokenConfig;
import com.model.gateway.auth.identity.domain.model.LoginUser;
import com.model.gateway.auth.newapi.domain.model.UserNewApiBindingLog;
import com.model.gateway.auth.identity.domain.model.SysUser;
import com.model.gateway.auth.newapi.domain.model.UserNewApiBinding;
import com.model.gateway.auth.identity.interfaces.dto.UserAmountUpdateRequest;
import com.model.gateway.auth.identity.interfaces.dto.AdminPasswordResetRequest;
import com.model.gateway.auth.identity.interfaces.dto.AdminUserCreateRequest;
import com.model.gateway.auth.identity.interfaces.dto.UserCreateRequest;
import com.model.gateway.auth.identity.interfaces.dto.UserPasswordUpdateRequest;
import com.model.gateway.auth.identity.interfaces.dto.UserProfileUpdateRequest;
import com.model.gateway.auth.shared.exception.AuthException;
import com.model.gateway.auth.identity.infrastructure.persistence.mapper.UserMapper;
import com.model.gateway.auth.newapi.infrastructure.persistence.mapper.UserNewApiBindingMapper;
import com.model.gateway.auth.newapi.infrastructure.persistence.mapper.UserNewApiBindingLogMapper;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserDetailVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListItemVo;
import com.model.gateway.auth.identity.interfaces.vo.AdminUserListPageVo;
import com.model.gateway.auth.identity.interfaces.vo.UserCreateResponse;
import com.model.gateway.auth.identity.interfaces.vo.UserProfileVo;
import com.model.gateway.auth.identity.interfaces.vo.UserTokenRecordsVo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 个人用户资料业务服务。
 */
@Service
public class UserProfileApplicationService {

    /**
     * new-api用户名最大长度。
     */
    private static final int NEW_API_USERNAME_MAX_LENGTH = 20;

    /**
     * new-api用户名后缀长度。
     */
    private static final int NEW_API_USERNAME_SUFFIX_LENGTH = 6;

    /**
     * new-api用户名创建最大重试次数。
     */
    private static final int NEW_API_CREATE_RETRY_TIMES = 3;

    /**
     * 最小密码长度。
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * 额度锁有效期秒数。
     */
    private static final long QUOTA_LOCK_TTL_SECONDS = 30L;

    /**
     * 额度模式增加。
     */
    private static final String QUOTA_MODE_ADD = "add";

    /**
     * 额度模式减少。
     */
    private static final String QUOTA_MODE_SUBTRACT = "subtract";

    /**
     * 额度模式覆盖。
     */
    private static final String QUOTA_MODE_OVERRIDE = "override";

    /**
     * 随机字符表。
     */
    private static final char[] RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * new-api绑定数据访问对象。
     */
    private final UserNewApiBindingMapper bindingMapper;

    /**
     * new-api绑定日志数据访问对象。
     */
    private final UserNewApiBindingLogMapper bindingLogMapper;

    /**
     * new-api外部用户管理接口ACL。
     */
    private final NewApiUserAcl newApiUserAcl;

    /**
     * new-api绑定业务服务。
     */
    private final NewApiBindingApplicationService newApiBindingService;

    /**
     * 网关凭证Redis缓存服务。
     */
    private final GatewayCredentialCacheService gatewayCredentialCacheService;

    /**
     * BCrypt密码编码器。
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * RBAC应用服务。
     */
    private final RbacApplicationService rbacApplicationService;

    /**
     * new-api外部用户管理接口配置。
     */
    private final NewApiUserManagerProperties newApiUserManagerProperties;

    /**
     * 事务模板。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 安全随机数。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建个人用户资料业务服务。
     *
     * @param userMapper 用户数据访问对象
     * @param bindingMapper new-api绑定数据访问对象
     * @param bindingLogMapper new-api绑定日志数据访问对象
     * @param newApiUserAcl new-api外部用户管理接口ACL
     * @param newApiBindingService new-api绑定业务服务
     * @param gatewayCredentialCacheService 网关凭证Redis缓存服务
     * @param passwordEncoder BCrypt密码编码器
     * @param rbacApplicationService RBAC应用服务
     * @param newApiUserManagerProperties new-api外部用户管理接口配置
     * @param transactionTemplate 事务模板
     */
    public UserProfileApplicationService(
            UserMapper userMapper,
            UserNewApiBindingMapper bindingMapper,
            UserNewApiBindingLogMapper bindingLogMapper,
            NewApiUserAcl newApiUserAcl,
            NewApiBindingApplicationService newApiBindingService,
            GatewayCredentialCacheService gatewayCredentialCacheService,
            BCryptPasswordEncoder passwordEncoder,
            RbacApplicationService rbacApplicationService,
            NewApiUserManagerProperties newApiUserManagerProperties,
            TransactionTemplate transactionTemplate) {
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.bindingLogMapper = bindingLogMapper;
        this.newApiUserAcl = newApiUserAcl;
        this.newApiBindingService = newApiBindingService;
        this.gatewayCredentialCacheService = gatewayCredentialCacheService;
        this.passwordEncoder = passwordEncoder;
        this.rbacApplicationService = rbacApplicationService;
        this.newApiUserManagerProperties = newApiUserManagerProperties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 从SaSession读取当前登录上下文用户。
     *
     * @return 登录上下文用户
     */
    private LoginUser currentLoginUser() {
        return (LoginUser) StpUtil.getSession().get(SaTokenConfig.SESSION_LOGIN_USER_KEY);
    }

    /**
     * 查询当前用户资料。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 当前用户资料
     */
    public UserProfileVo getProfile() {
        LoginUser user = currentLoginUser();
        UserNewApiBinding binding = newApiBindingService.getBinding(user.getUserId());
        NewApiUserAcl.NewApiUserStatsData stats = newApiUserAcl.getUserStats(binding.getNewApiUserId(), null, null);
        NewApiUserAcl.AccountData accountData = stats.getAccountData();
        return UserProfileVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .roles(rbacApplicationService.getRoleCodes(user.getUserId()))
                .status(user.getStatus())
                .newApiUserId(binding.getNewApiUserId())
                .newApiUserName(binding.getNewApiUserName())
                .currentBalanceAmount(accountData == null ? null : accountData.getCurrentBalanceAmount())
                .usedQuotaAmount(accountData == null ? null : accountData.getUsedQuotaAmount())
                .totalQuotaAmount(accountData == null ? null : accountData.getTotalQuotaAmount())
                .quota(accountData == null ? null : accountData.getQuota())
                .usedQuota(accountData == null ? null : accountData.getUsedQuota())
                .totalQuota(accountData == null ? null : accountData.getTotalQuota())
                .quotaPerUnit(accountData == null ? null : accountData.getQuotaPerUnit())
                .build();
    }

    /**
     * 创建系统用户(公开注册,强制绑定 new-api)。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    public UserCreateResponse createUser(UserCreateRequest request) {
        UserCreateResponse response = createUserInternal(request, false);
        adminBindNewApi(response.getUserId());
        return UserCreateResponse.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .newApiBound(Boolean.TRUE)
                .build();
    }

    /**
     * 管理员创建用户。
     *
     * @param request 管理员创建用户请求
     * @return 创建用户响应
     */
    public UserCreateResponse adminCreateUser(AdminUserCreateRequest request) {
        UserCreateRequest userRequest = UserCreateRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        boolean bindNewApi = request.getBindNewApi() == null || request.getBindNewApi();
        UserCreateResponse response = createUserInternal(userRequest, false);
        if (bindNewApi) {
            adminBindNewApi(response.getUserId());
        }
        return UserCreateResponse.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .newApiBound(bindNewApi)
                .build();
    }

    /**
     * 更新当前用户基本资料。
     *
     * @param request 用户资料更新请求
     */
    public void updateProfile(UserProfileUpdateRequest request) {
        checkProfileUpdateRequest(request);
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        checkPhoneAvailable(userId, request.getPhone());
        checkEmailAvailable(userId, request.getEmail());
        SysUser updateUser = SysUser.builder()
                .userId(userId)
                .nickname(request.getNickname().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail().trim())
                .build();
        userMapper.updateProfile(updateUser);
        SysUser refreshedUser = userMapper.selectByUserId(userId);
        LoginUser loginUser = LoginUser.from(refreshedUser);
        loginUser.setRoles(rbacApplicationService.getRoleCodes(userId));
        StpUtil.getSession().set(SaTokenConfig.SESSION_LOGIN_USER_KEY, loginUser);
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 密码修改请求
     */
    public void updatePassword(UserPasswordUpdateRequest request) {
        checkPasswordUpdateRequest(request);
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AuthException("旧密码错误");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 管理员重置用户密码。
     *
     * @param userId 用户ID
     * @param request 密码重置请求
     */
    public void adminResetPassword(Long userId, AdminPasswordResetRequest request) {
        checkResetPasswordRequest(request);
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 内部创建用户逻辑。
     *
     * @param request 创建用户请求
     * @param bindNewApi 是否绑定 new-api
     * @return 创建用户响应
     */
    private UserCreateResponse createUserInternal(UserCreateRequest request, boolean bindNewApi) {
        checkCreateRequest(request);
        RegisterContext context = createPendingUser(request);
        if (bindNewApi) {
            bindNewApiUser(context, request);
        }
        return UserCreateResponse.builder()
                .userId(context.getUserId())
                .username(context.getUsername())
                .newApiBound(bindNewApi)
                .build();
    }

    /**
     * 查询当前用户Token使用记录。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    public UserTokenRecordsVo getTokenRecords(
            Integer page,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserNewApiBinding binding = newApiBindingService.getBinding(userId);
        NewApiUserAcl.NewApiQuotaRecordsData records = newApiUserAcl.getQuotaRecords(
                binding.getNewApiUserId(),
                page,
                pageSize,
                startTimestamp,
                endTimestamp,
                modelName
        );
        List<NewApiUserAcl.QuotaRecordItem> items = records.getItems() == null
                ? Collections.emptyList()
                : records.getItems();
        return UserTokenRecordsVo.builder()
                .page(records.getPage())
                .pageSize(records.getPageSize())
                .total(records.getTotal())
                .items(items.stream()
                        .map(this::buildTokenRecordItem)
                        .toList())
                .build();
    }

    /**
     * 查询当前用户可用模型。当前登录上下文由拦截器写入持有者，无需透传Authorization。
     *
     * @return 可用模型列表
     */
    public List<String> getModels() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserNewApiBinding binding = newApiBindingService.getBinding(userId);
        return newApiUserAcl.getUserModels(binding.getNewApiUserId());
    }

    /**
     * 管理员修改用户状态。
     *
     * @param userId 用户ID
     * @param status 目标状态
     */
    public void adminUpdateStatus(Long userId, Integer status) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        userMapper.updateStatus(userId, status);
    }

    /**
     * 管理员为已有用户补绑 new-api。
     *
     * @param userId 用户ID
     */
    public void adminBindNewApi(Long userId) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        if (binding != null && binding.getNewApiUserId() != null) {
            throw new AuthException("用户已绑定 new-api");
        }
        Long bindingId;
        if (binding == null) {
            UserNewApiBinding newBinding = UserNewApiBinding.builder()
                    .userId(userId)
                    .status(UserStatusEnum.PENDING.getCode())
                    .build();
            bindingMapper.insert(newBinding);
            bindingId = newBinding.getId();
            insertBindingLog(userId, bindingId, "CREATE_BINDING", true, "创建待绑定用户");
        } else {
            bindingId = binding.getId();
        }
        RegisterContext context = RegisterContext.builder()
                .userId(userId)
                .username(user.getUsername())
                .bindingId(bindingId)
                .build();
        UserCreateRequest request = UserCreateRequest.builder()
                .username(user.getUsername())
                .password(newApiUserManagerProperties.getDefaultPassword())
                .nickname(user.getNickname())
                .build();
        bindNewApiUser(context, request);
    }

    /**
     * 管理员查询用户列表。
     *
     * @param username 用户名(模糊)
     * @param role 角色
     * @param status 状态
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 用户列表分页
     */
    public AdminUserListPageVo adminListUsers(String username, String role, Integer status, int pageNo, int pageSize) {
        int offset = (pageNo - 1) * pageSize;
        long total = userMapper.selectCountByCondition(username, role, status);
        List<SysUser> users = userMapper.selectListByCondition(username, role, status, offset, pageSize);
        List<AdminUserListItemVo> list = users.stream()
                .map(this::buildAdminListItem)
                .toList();
        return AdminUserListPageVo.builder()
                .list(list)
                .total(total)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .build();
    }

    /**
     * 构建管理员列表项。
     *
     * @param user 系统用户
     * @return 管理员列表项
     */
    private AdminUserListItemVo buildAdminListItem(SysUser user) {
        UserNewApiBinding binding = bindingMapper.selectByUserId(user.getUserId());
        boolean newApiBound = binding != null && binding.getNewApiUserId() != null;
        return AdminUserListItemVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .roles(rbacApplicationService.getRoleCodes(user.getUserId()))
                .status(user.getStatus())
                .newApiBound(newApiBound)
                .build();
    }

    /**
     * 管理员查询用户详情。
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public AdminUserDetailVo adminGetUserDetail(Long userId) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        boolean newApiBound = binding != null && binding.getNewApiUserId() != null;

        AdminUserDetailVo.AdminUserDetailVoBuilder builder = AdminUserDetailVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .roles(rbacApplicationService.getRoleCodes(user.getUserId()))
                .status(user.getStatus())
                .newApiBound(newApiBound);

        if (binding != null) {
            builder.newApiUserId(binding.getNewApiUserId())
                    .newApiUserName(binding.getNewApiUserName())
                    .newApiStatus(binding.getStatus());
        }

        if (newApiBound) {
            NewApiUserAcl.NewApiUserStatsData stats = newApiUserAcl.getUserStats(binding.getNewApiUserId(), null, null);
            NewApiUserAcl.AccountData accountData = stats.getAccountData();
            if (accountData != null) {
                builder.currentBalanceAmount(accountData.getCurrentBalanceAmount())
                        .usedQuotaAmount(accountData.getUsedQuotaAmount())
                        .totalQuotaAmount(accountData.getTotalQuotaAmount())
                        .quota(accountData.getQuota())
                        .usedQuota(accountData.getUsedQuota())
                        .totalQuota(accountData.getTotalQuota())
                        .quotaPerUnit(accountData.getQuotaPerUnit());
            }
        }

        return builder.build();
    }

    /**
     * 管理员查询用户Token使用记录。
     *
     * @param userId 用户ID
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    public UserTokenRecordsVo adminGetTokenRecords(
            Long userId,
            Integer pageNo,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = bindingMapper.selectByUserId(userId);
        if (binding == null || binding.getNewApiUserId() == null) {
            return UserTokenRecordsVo.builder()
                    .page(pageNo != null ? pageNo : 1)
                    .pageSize(pageSize != null ? pageSize : 10)
                    .total(0L)
                    .items(Collections.emptyList())
                    .build();
        }
        NewApiUserAcl.NewApiQuotaRecordsData records = newApiUserAcl.getQuotaRecords(
                binding.getNewApiUserId(), pageNo, pageSize, startTimestamp, endTimestamp, modelName);
        List<NewApiUserAcl.QuotaRecordItem> items = records.getItems() == null
                ? Collections.emptyList()
                : records.getItems();
        return UserTokenRecordsVo.builder()
                .page(records.getPage())
                .pageSize(records.getPageSize())
                .total(records.getTotal())
                .items(items.stream().map(this::buildTokenRecordItem).toList())
                .build();
    }

    /**
     * 管理员查询用户可用模型。
     *
     * @param userId 用户ID
     * @return 可用模型列表
     */
    public List<String> adminGetModels(Long userId) {
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        UserNewApiBinding binding = newApiBindingService.getBinding(userId);
        return newApiUserAcl.getUserModels(binding.getNewApiUserId());
    }

    /**
     * 管理员设置用户金额。
     *
     * @param userId 业务用户ID
     * @param request 金额修改请求
     */
    public void updateUserAmount(Long userId, UserAmountUpdateRequest request) {
        checkAmountRequest(request);
        String lockValue = UUID.randomUUID().toString();
        if (!gatewayCredentialCacheService.tryLockUserQuota(userId, lockValue, QUOTA_LOCK_TTL_SECONDS)) {
            throw new AuthException("用户额度正在变更，请稍后重试");
        }
        try {
            SysUser user = userMapper.selectByUserId(userId);
            checkUser(user);
            UserNewApiBinding binding = newApiBindingService.getBinding(userId);
            NewApiUserAcl.NewApiUserStatsData stats = newApiUserAcl.getUserStats(binding.getNewApiUserId(), null, null);
            Long quotaPerUnit = resolveQuotaPerUnit(stats);
            Long quotaValue = convertAmountToQuota(request.getAmount(), quotaPerUnit);
            newApiUserAcl.setUserQuota(binding.getNewApiUserId(), request.getMode(), quotaValue);
            insertBindingLog(userId, binding.getId(), "UPDATE_QUOTA", true, "设置用户金额成功");
        } catch (RuntimeException exception) {
            insertBindingLog(userId, null, "UPDATE_QUOTA", false, exception.getMessage());
            throw exception;
        } finally {
            gatewayCredentialCacheService.unlockUserQuota(userId, lockValue);
        }
    }

    /**
     * 校验创建用户请求。
     *
     * @param request 创建用户请求
     */
    private void checkCreateRequest(UserCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new AuthException("用户名和密码不能为空");
        }
    }

    /**
     * 校验资料更新请求。
     *
     * @param request 用户资料更新请求
     */
    private void checkProfileUpdateRequest(UserProfileUpdateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getNickname())
                || !StringUtils.hasText(request.getPhone())
                || !StringUtils.hasText(request.getEmail())) {
            throw new AuthException("昵称、手机号和邮箱不能为空");
        }
    }

    /**
     * 校验密码修改请求。
     *
     * @param request 密码修改请求
     */
    private void checkPasswordUpdateRequest(UserPasswordUpdateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new AuthException("旧密码和新密码不能为空");
        }
        checkNewPassword(request.getNewPassword());
    }

    /**
     * 校验密码重置请求。
     *
     * @param request 密码重置请求
     */
    private void checkResetPasswordRequest(AdminPasswordResetRequest request) {
        if (request == null || !StringUtils.hasText(request.getNewPassword())) {
            throw new AuthException("新密码不能为空");
        }
        checkNewPassword(request.getNewPassword());
    }

    /**
     * 校验新密码强度。
     *
     * @param newPassword 新明文密码
     */
    private void checkNewPassword(String newPassword) {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("新密码长度不能少于6位");
        }
    }

    /**
     * 校验手机号未被其他用户占用。
     *
     * @param userId 当前用户ID
     * @param phone 手机号
     */
    private void checkPhoneAvailable(Long userId, String phone) {
        SysUser exists = userMapper.selectByPhone(phone.trim());
        if (exists != null && !exists.getUserId().equals(userId)) {
            throw new AuthException("手机号已存在");
        }
    }

    /**
     * 校验邮箱未被其他用户占用。
     *
     * @param userId 当前用户ID
     * @param email 邮箱
     */
    private void checkEmailAvailable(Long userId, String email) {
        SysUser exists = userMapper.selectByEmail(email.trim());
        if (exists != null && !exists.getUserId().equals(userId)) {
            throw new AuthException("邮箱已存在");
        }
    }

    /**
     * 创建待绑定本地用户。
     *
     * @param request 创建用户请求
     * @return 注册上下文
     */
    private RegisterContext createPendingUser(UserCreateRequest request) {
        return transactionTemplate.execute(status -> {
            SysUser exists = userMapper.selectByUsername(request.getUsername());
            if (exists != null) {
                throw new AuthException("用户名已存在");
            }
            if (StringUtils.hasText(request.getPhone())) {
                SysUser phoneExists = userMapper.selectByPhone(request.getPhone());
                if (phoneExists != null) {
                    throw new AuthException("手机号已存在");
                }
            }
            if (StringUtils.hasText(request.getEmail())) {
                SysUser emailExists = userMapper.selectByEmail(request.getEmail());
                if (emailExists != null) {
                    throw new AuthException("邮箱已存在");
                }
            }
            SysUser user = SysUser.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .nickname(request.getNickname())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .status(UserStatusEnum.DISABLE.getCode())
                    .build();
            userMapper.insert(user);
            rbacApplicationService.assignDefaultUserRole(user.getUserId());
            UserNewApiBinding binding = UserNewApiBinding.builder()
                    .userId(user.getUserId())
                    .status(UserStatusEnum.PENDING.getCode())
                    .build();
            bindingMapper.insert(binding);
            insertBindingLog(user.getUserId(), binding.getId(), "CREATE_BINDING", true, "创建待绑定用户");
            return RegisterContext.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .bindingId(binding.getId())
                    .build();
        });
    }

    /**
     * 绑定new-api用户。
     *
     * @param context 注册上下文
     * @param request 创建用户请求
     */
    private void bindNewApiUser(RegisterContext context, UserCreateRequest request) {
        try {
            NewApiUserAcl.NewApiCreateUserData newApiUser = createNewApiUserWithRetry(context, request);
            transactionTemplate.executeWithoutResult(status -> {
                bindingMapper.updateBinding(UserNewApiBinding.builder()
                        .id(context.getBindingId())
                        .newApiUserId(newApiUser.getUserId())
                        .newApiUserName(newApiUser.getUsername())
                        .newApiApiKey(newApiUser.getTokenKey())
                        .status(UserStatusEnum.ENABLE.getCode())
                        .build());
                userMapper.updateStatus(context.getUserId(), UserStatusEnum.ENABLE.getCode());
                insertBindingLog(context.getUserId(), context.getBindingId(), "CREATE_BINDING", true, "new-api用户创建成功");
            });
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                bindingMapper.updateStatus(context.getBindingId(), UserStatusEnum.ERROR.getCode());
                insertBindingLog(context.getUserId(), context.getBindingId(), "SYNC_FAILED", false, exception.getMessage());
            });
            throw exception;
        }
    }

    /**
     * 重试创建new-api用户。
     *
     * @param context 注册上下文
     * @param request 创建用户请求
     * @return new-api创建用户数据
     */
    private NewApiUserAcl.NewApiCreateUserData createNewApiUserWithRetry(RegisterContext context, UserCreateRequest request) {
        RuntimeException lastException = null;
        for (int index = 0; index < NEW_API_CREATE_RETRY_TIMES; index++) {
            String newApiUsername = buildNewApiUsername(context.getUsername(), context.getUserId());
            try {
                return newApiUserAcl.createUser(
                        newApiUsername,
                        request.getPassword(),
                        StringUtils.hasText(request.getNickname()) ? request.getNickname() : context.getUsername()
                );
            } catch (RuntimeException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new AuthException("创建new-api用户失败") : lastException;
    }

    /**
     * 构建new-api用户名。
     *
     * @param username 业务用户名
     * @param userId 业务用户ID
     * @return new-api用户名
     */
    private String buildNewApiUsername(String username, Long userId) {
        String suffix = randomSuffix();
        String tail = "_" + userId + "_" + suffix;
        int prefixLength = Math.max(1, NEW_API_USERNAME_MAX_LENGTH - tail.length());
        String prefix = sanitizeUsername(username);
        if (prefix.length() > prefixLength) {
            prefix = prefix.substring(0, prefixLength);
        }
        String result = prefix + tail;
        if (result.length() > NEW_API_USERNAME_MAX_LENGTH) {
            return result.substring(result.length() - NEW_API_USERNAME_MAX_LENGTH);
        }
        return result;
    }

    /**
     * 清理new-api用户名。
     *
     * @param username 原始用户名
     * @return 可用用户名片段
     */
    private String sanitizeUsername(String username) {
        String value = username.replaceAll("[^A-Za-z0-9_]", "_").toLowerCase(Locale.ROOT);
        return StringUtils.hasText(value) ? value : "u";
    }

    /**
     * 生成随机后缀。
     *
     * @return 随机后缀
     */
    private String randomSuffix() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < NEW_API_USERNAME_SUFFIX_LENGTH; index++) {
            builder.append(RANDOM_CHARS[secureRandom.nextInt(RANDOM_CHARS.length)]);
        }
        return builder.toString();
    }

    /**
     * 构建Token记录项。
     *
     * @param item new-api记录项
     * @return Token记录项
     */
    private UserTokenRecordsVo.UserTokenRecordItemVo buildTokenRecordItem(NewApiUserAcl.QuotaRecordItem item) {
        return UserTokenRecordsVo.UserTokenRecordItemVo.builder()
                .id(item.getId())
                .newApiUserId(item.getUserId())
                .newApiUserName(item.getUsername())
                .modelName(item.getModelName())
                .createdAt(item.getCreatedAt())
                .tokenUsed(item.getTokenUsed())
                .count(item.getCount())
                .quota(item.getQuota())
                .build();
    }

    /**
     * 校验用户金额修改请求。
     *
     * @param request 用户金额修改请求
     */
    private void checkAmountRequest(UserAmountUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getMode()) || request.getAmount() == null) {
            throw new AuthException("金额修改参数不能为空");
        }
        String mode = request.getMode();
        if (!QUOTA_MODE_ADD.equals(mode) && !QUOTA_MODE_SUBTRACT.equals(mode) && !QUOTA_MODE_OVERRIDE.equals(mode)) {
            throw new AuthException("金额操作模式不正确");
        }
        if ((QUOTA_MODE_ADD.equals(mode) || QUOTA_MODE_SUBTRACT.equals(mode))
                && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuthException("增加或减少金额必须大于0");
        }
        if (QUOTA_MODE_OVERRIDE.equals(mode) && request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new AuthException("覆盖金额不能小于0");
        }
    }

    /**
     * 校验用户状态。
     *
     * @param user 用户信息
     */
    private void checkUser(SysUser user) {
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }
    }

    /**
     * 解析额度金额换算比例。
     *
     * @param stats new-api统计数据
     * @return 换算比例
     */
    private Long resolveQuotaPerUnit(NewApiUserAcl.NewApiUserStatsData stats) {
        if (stats == null || stats.getAccountData() == null || stats.getAccountData().getQuotaPerUnit() == null) {
            throw new AuthException("new-api额度换算比例不存在");
        }
        return stats.getAccountData().getQuotaPerUnit();
    }

    /**
     * 金额转换为原始额度。
     *
     * @param amount 金额元
     * @param quotaPerUnit 换算比例
     * @return 原始额度
     */
    private Long convertAmountToQuota(BigDecimal amount, Long quotaPerUnit) {
        try {
            return amount.multiply(BigDecimal.valueOf(quotaPerUnit))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new AuthException("金额超过可支持范围");
        }
    }

    /**
     * 写入绑定操作日志。
     *
     * @param userId 业务用户ID
     * @param bindingId 绑定ID
     * @param operateType 操作类型
     * @param success 是否成功
     * @param message 操作说明
     */
    private void insertBindingLog(Long userId, Long bindingId, String operateType, Boolean success, String message) {
        bindingLogMapper.insert(UserNewApiBindingLog.builder()
                .userId(userId)
                .bindingId(bindingId)
                .operateType(operateType)
                .success(success)
                .message(message == null ? null : message.substring(0, Math.min(message.length(), 1024)))
                .build());
    }

    /**
     * 注册上下文。
     */
    @lombok.Data
    @lombok.Builder
    private static class RegisterContext {

        /**
         * 业务用户ID。
         */
        private Long userId;

        /**
         * 业务用户名。
         */
        private String username;

        /**
         * 绑定ID。
         */
        private Long bindingId;
    }
}
