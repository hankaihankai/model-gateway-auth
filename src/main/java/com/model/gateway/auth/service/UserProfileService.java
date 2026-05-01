package com.model.gateway.auth.service;

import com.model.gateway.auth.acl.NewApiUserAcl;
import com.model.gateway.auth.common.AuthConstants;
import com.model.gateway.auth.common.UserStatusEnum;
import com.model.gateway.auth.domain.UserNewApiBindingLog;
import com.model.gateway.auth.domain.SysUser;
import com.model.gateway.auth.domain.UserNewApiBinding;
import com.model.gateway.auth.dto.UserAmountUpdateRequest;
import com.model.gateway.auth.dto.UserCreateRequest;
import com.model.gateway.auth.exception.AuthException;
import com.model.gateway.auth.mapper.UserMapper;
import com.model.gateway.auth.mapper.UserNewApiBindingMapper;
import com.model.gateway.auth.mapper.UserNewApiBindingLogMapper;
import com.model.gateway.auth.vo.UserCreateResponse;
import com.model.gateway.auth.vo.UserProfileVo;
import com.model.gateway.auth.vo.UserTokenRecordsVo;
import io.jsonwebtoken.Claims;
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
public class UserProfileService {

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
     * 网关JWT服务。
     */
    private final GatewayJwtService gatewayJwtService;

    /**
     * new-api绑定业务服务。
     */
    private final NewApiBindingService newApiBindingService;

    /**
     * 网关凭证Redis缓存服务。
     */
    private final GatewayCredentialCacheService gatewayCredentialCacheService;

    /**
     * BCrypt密码编码器。
     */
    private final BCryptPasswordEncoder passwordEncoder;

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
     * @param gatewayJwtService 网关JWT服务
     * @param newApiBindingService new-api绑定业务服务
     * @param gatewayCredentialCacheService 网关凭证Redis缓存服务
     * @param passwordEncoder BCrypt密码编码器
     * @param transactionTemplate 事务模板
     */
    public UserProfileService(
            UserMapper userMapper,
            UserNewApiBindingMapper bindingMapper,
            UserNewApiBindingLogMapper bindingLogMapper,
            NewApiUserAcl newApiUserAcl,
            GatewayJwtService gatewayJwtService,
            NewApiBindingService newApiBindingService,
            GatewayCredentialCacheService gatewayCredentialCacheService,
            BCryptPasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {
        this.userMapper = userMapper;
        this.bindingMapper = bindingMapper;
        this.bindingLogMapper = bindingLogMapper;
        this.newApiUserAcl = newApiUserAcl;
        this.gatewayJwtService = gatewayJwtService;
        this.newApiBindingService = newApiBindingService;
        this.gatewayCredentialCacheService = gatewayCredentialCacheService;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 查询当前用户资料。
     *
     * @param authorization Authorization请求头
     * @return 当前用户资料
     */
    public UserProfileVo getProfile(String authorization) {
        Claims claims = gatewayJwtService.parseToken(gatewayJwtService.extractBearerToken(authorization));
        Long userId = Long.valueOf(claims.getSubject());
        SysUser user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new AuthException("用户已禁用");
        }
        UserNewApiBinding binding = newApiBindingService.getBinding(userId);
        NewApiUserAcl.NewApiUserStatsData stats = newApiUserAcl.getUserStats(binding.getNewApiUserId(), null, null);
        NewApiUserAcl.AccountData accountData = stats.getAccountData();
        return UserProfileVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
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
     * 创建系统用户。
     *
     * @param request 创建用户请求
     * @return 创建用户响应
     */
    public UserCreateResponse createUser(UserCreateRequest request) {
        checkCreateRequest(request);
        RegisterContext context = createPendingUser(request);
        bindNewApiUser(context, request);
        return UserCreateResponse.builder()
                .userId(context.getUserId())
                .username(context.getUsername())
                .newApiBound(Boolean.TRUE)
                .build();
    }

    /**
     * 查询当前用户Token使用记录。
     *
     * @param authorization Authorization请求头
     * @param page 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return Token使用记录分页
     */
    public UserTokenRecordsVo getTokenRecords(
            String authorization,
            Integer page,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        Long userId = parseCurrentUserId(authorization);
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
     * 创建待绑定本地用户。
     *
     * @param request 创建用户请求
     * @return 注册上下文
     */
    private RegisterContext createPendingUser(UserCreateRequest request) {
        return transactionTemplate.execute(status -> {
            SysUser exists = userMapper.selectByUsername(request.getUsername());
            if (exists != null) {
                throw new AuthException("用户名已存在或正在创建中");
            }
            SysUser user = SysUser.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .nickname(request.getNickname())
                    .role(AuthConstants.ROLE_USER)
                    .status(UserStatusEnum.DISABLE.getCode())
                    .build();
            userMapper.insert(user);
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
     * 解析当前用户ID。
     *
     * @param authorization Authorization请求头
     * @return 当前用户ID
     */
    private Long parseCurrentUserId(String authorization) {
        Claims claims = gatewayJwtService.parseToken(gatewayJwtService.extractBearerToken(authorization));
        return Long.valueOf(claims.getSubject());
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
