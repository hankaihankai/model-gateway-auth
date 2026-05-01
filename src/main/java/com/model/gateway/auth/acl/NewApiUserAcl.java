package com.model.gateway.auth.acl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.gateway.auth.config.NewApiUserManagerProperties;
import com.model.gateway.auth.exception.AuthException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * new-api外部用户管理接口ACL。
 */
@Component
public class NewApiUserAcl {

    /**
     * 创建用户接口路径。
     */
    private static final String USERS_PATH = "/api/user-manager/users";

    /**
     * 用户统计接口路径模板。
     */
    private static final String USER_STATS_PATH = "/api/user-manager/users/{userId}/stats";

    /**
     * 用户额度记录接口路径模板。
     */
    private static final String USER_QUOTA_RECORDS_PATH = "/api/user-manager/users/{userId}/quota/records";

    /**
     * 用户额度设置接口路径模板。
     */
    private static final String USER_QUOTA_PATH = "/api/user-manager/users/{userId}/quota";

    /**
     * new-api外部用户管理接口配置。
     */
    private final NewApiUserManagerProperties properties;

    /**
     * REST客户端。
     */
    private final RestClient restClient;

    /**
     * JSON对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建new-api外部用户管理接口ACL。
     *
     * @param properties new-api外部用户管理接口配置
     * @param restClientBuilder REST客户端构建器
     * @param objectMapper JSON对象映射器
     */
    public NewApiUserAcl(
            NewApiUserManagerProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .requestFactory(buildRequestFactory(properties.getTimeoutMillis()))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 创建new-api用户并生成默认令牌。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param displayName 显示名称
     * @return new-api创建用户数据
     */
    public NewApiCreateUserData createUser(String username, String password, String displayName) {
        checkConfig();
        NewApiCreateUserRequest request = NewApiCreateUserRequest.builder()
                .username(username)
                .password(password)
                .displayName(StringUtils.hasText(displayName) ? displayName : username)
                .build();
        NewApiResponse<NewApiCreateUserData> response = execute(() -> restClient.post()
                        .uri(buildUrl(USERS_PATH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthKey())
                        .body(request)
                        .retrieve()
                        .body(new ParameterizedTypeReference<NewApiResponse<NewApiCreateUserData>>() {
                        }),
                "创建new-api用户失败");
        return readData(response, "创建new-api用户失败");
    }

    /**
     * 查询new-api用户统计。
     *
     * @param userId new-api用户ID
     * @param startTimestamp 统计开始Unix时间戳秒
     * @param endTimestamp 统计结束Unix时间戳秒
     * @return new-api用户统计数据
     */
    public NewApiUserStatsData getUserStats(Long userId, Long startTimestamp, Long endTimestamp) {
        checkConfig();
        NewApiResponse<NewApiUserStatsData> response = execute(() -> restClient.get()
                        .uri(buildStatsUri(userId, startTimestamp, endTimestamp))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthKey())
                        .retrieve()
                        .body(new ParameterizedTypeReference<NewApiResponse<NewApiUserStatsData>>() {
                        }),
                "查询new-api用户统计失败");
        return readData(response, "查询new-api用户统计失败");
    }

    /**
     * 查询new-api用户额度调用记录。
     *
     * @param userId new-api用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return new-api额度调用记录数据
     */
    public NewApiQuotaRecordsData getQuotaRecords(
            Long userId,
            Integer page,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        checkConfig();
        NewApiResponse<NewApiQuotaRecordsData> response = execute(() -> restClient.get()
                        .uri(buildQuotaRecordsUri(userId, page, pageSize, startTimestamp, endTimestamp, modelName))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthKey())
                        .retrieve()
                        .body(new ParameterizedTypeReference<NewApiResponse<NewApiQuotaRecordsData>>() {
                        }),
                "查询new-api额度记录失败");
        return readData(response, "查询new-api额度记录失败");
    }

    /**
     * 设置new-api用户额度。
     *
     * @param userId new-api用户ID
     * @param mode 操作模式
     *
    - `add`：在用户当前额度基础上增加 `value`。
    - `subtract`：在用户当前额度基础上减少 `value`；若用户额度不足，会返回错误。
    - `override`：直接将用户额度覆盖为 `value`，不依赖当前额度。
     * @param value 额度值
     */
    public void setUserQuota(Long userId, String mode, Long value) {
        checkConfig();
        NewApiSetUserQuotaRequest request = NewApiSetUserQuotaRequest.builder()
                .mode(mode)
                .value(value)
                .build();
        NewApiResponse<Void> response = execute(() -> restClient.post()
                        .uri(buildUrl(USER_QUOTA_PATH.replace("{userId}", userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthKey())
                        .body(request)
                        .retrieve()
                        .body(new ParameterizedTypeReference<NewApiResponse<Void>>() {
                        }),
                "设置new-api用户额度失败");
        readSuccess(response, "设置new-api用户额度失败");
    }

    /**
     * 构建HTTP请求工厂。
     *
     * @param timeoutMillis 超时时间毫秒数
     * @return HTTP请求工厂
     */
    private SimpleClientHttpRequestFactory buildRequestFactory(Integer timeoutMillis) {
        int timeout = timeoutMillis == null ? 5000 : timeoutMillis;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));
        return factory;
    }

    /**
     * 校验new-api外部用户管理接口配置。
     */
    private void checkConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new AuthException("new-api服务地址未配置");
        }
        if (!StringUtils.hasText(properties.getAuthKey())) {
            throw new AuthException("new-api用户管理授权码未配置");
        }
    }

    /**
     * 执行new-api HTTP请求。
     *
     * @param supplier 请求执行器
     * @param defaultMessage 默认错误消息
     * @param <T> 响应数据类型
     * @return new-api统一响应
     */
    private <T> NewApiResponse<T> execute(Supplier<NewApiResponse<T>> supplier, String defaultMessage) {
        try {
            return supplier.get();
        } catch (RestClientResponseException exception) {
            throw new AuthException(readErrorMessage(exception, defaultMessage));
        } catch (RestClientException exception) {
            throw new AuthException(defaultMessage + ": " + exception.getMessage());
        }
    }

    /**
     * 读取new-api错误响应消息。
     *
     * @param exception HTTP响应异常
     * @param defaultMessage 默认错误消息
     * @return 错误消息
     */
    private String readErrorMessage(RestClientResponseException exception, String defaultMessage) {
        try {
            NewApiResponse<Void> response = objectMapper.readValue(
                    exception.getResponseBodyAsString(),
                    objectMapper.getTypeFactory().constructParametricType(NewApiResponse.class, Void.class)
            );
            if (response != null && StringUtils.hasText(response.getMessage())) {
                return response.getMessage();
            }
        } catch (JsonProcessingException ignored) {
            // 外部接口错误响应不是标准JSON时，使用默认错误消息。
        }
        return defaultMessage + ": HTTP " + exception.getStatusCode().value();
    }

    /**
     * 读取new-api响应数据。
     *
     * @param response new-api响应
     * @param defaultMessage 默认错误消息
     * @param <T> 数据类型
     * @return 响应数据
     */
    private <T> T readData(NewApiResponse<T> response, String defaultMessage) {
        if (response == null) {
            throw new AuthException(defaultMessage);
        }
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new AuthException(StringUtils.hasText(response.getMessage()) ? response.getMessage() : defaultMessage);
        }
        if (response.getData() == null) {
            throw new AuthException(defaultMessage);
        }
        return response.getData();
    }

    /**
     * 读取无数据new-api响应结果。
     *
     * @param response new-api响应
     * @param defaultMessage 默认错误消息
     */
    private void readSuccess(NewApiResponse<?> response, String defaultMessage) {
        if (response == null) {
            throw new AuthException(defaultMessage);
        }
        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new AuthException(StringUtils.hasText(response.getMessage()) ? response.getMessage() : defaultMessage);
        }
    }

    /**
     * 构建new-api接口完整URL。
     *
     * @param path 接口路径
     * @return 完整URL
     */
    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    /**
     * 构建用户统计URI。
     *
     * @param userId new-api用户ID
     * @param startTimestamp 统计开始Unix时间戳秒
     * @param endTimestamp 统计结束Unix时间戳秒
     * @return 用户统计URI
     */
    private URI buildStatsUri(Long userId, Long startTimestamp, Long endTimestamp) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                buildUrl(USER_STATS_PATH.replace("{userId}", userId.toString()))
        );
        if (startTimestamp != null) {
            builder.queryParam("start_timestamp", startTimestamp);
        }
        if (endTimestamp != null) {
            builder.queryParam("end_timestamp", endTimestamp);
        }
        return builder.build(true).toUri();
    }

    /**
     * 构建额度记录URI。
     *
     * @param userId new-api用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param startTimestamp 开始Unix时间戳秒
     * @param endTimestamp 结束Unix时间戳秒
     * @param modelName 模型名称
     * @return 额度记录URI
     */
    private URI buildQuotaRecordsUri(
            Long userId,
            Integer page,
            Integer pageSize,
            Long startTimestamp,
            Long endTimestamp,
            String modelName) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(
                buildUrl(USER_QUOTA_RECORDS_PATH.replace("{userId}", userId.toString()))
        );
        if (page != null) {
            builder.queryParam("p", page);
        }
        if (pageSize != null) {
            builder.queryParam("page_size", pageSize);
        }
        if (startTimestamp != null) {
            builder.queryParam("start_timestamp", startTimestamp);
        }
        if (endTimestamp != null) {
            builder.queryParam("end_timestamp", endTimestamp);
        }
        if (StringUtils.hasText(modelName)) {
            builder.queryParam("model_name", modelName);
        }
        return builder.build(true).toUri();
    }

    /**
     * new-api统一响应。
     *
     * @param <T> 数据类型
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiResponse<T> {

        /**
         * 是否成功。
         */
        private Boolean success;

        /**
         * 响应消息。
         */
        private String message;

        /**
         * 响应数据。
         */
        private T data;
    }

    /**
     * new-api创建用户请求。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiCreateUserRequest {

        /**
         * 用户名。
         */
        private String username;

        /**
         * 明文密码。
         */
        private String password;

        /**
         * 显示名称。
         */
        @JsonProperty("display_name")
        private String displayName;
    }

    /**
     * new-api创建用户响应数据。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiCreateUserData {

        /**
         * new-api用户ID。
         */
        @JsonProperty("user_id")
        private Long userId;

        /**
         * new-api用户名。
         */
        private String username;

        /**
         * 默认令牌名称。
         */
        @JsonProperty("token_name")
        private String tokenName;

        /**
         * 完整默认令牌。
         */
        @JsonProperty("token_key")
        private String tokenKey;
    }

    /**
     * new-api设置用户额度请求。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiSetUserQuotaRequest {

        /**
         * 操作模式。
         */
        private String mode;

        /**
         * 额度值。
         */
        private Long value;
    }

    /**
     * new-api用户统计响应数据。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiUserStatsData {

        /**
         * new-api用户ID。
         */
        @JsonProperty("user_id")
        private Long userId;

        /**
         * new-api用户名。
         */
        private String username;

        /**
         * 账户数据。
         */
        @JsonProperty("account_data")
        private AccountData accountData;

        /**
         * 使用统计。
         */
        @JsonProperty("usage_stats")
        private UsageStats usageStats;

        /**
         * 资源消耗。
         */
        @JsonProperty("resource_consumption")
        private ResourceConsumption resourceConsumption;

        /**
         * 性能指标。
         */
        @JsonProperty("performance_metrics")
        private PerformanceMetrics performanceMetrics;
    }

    /**
     * new-api账户数据。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountData {

        /**
         * 当前余额原始额度。
         */
        @JsonProperty("current_balance")
        private Long currentBalance;

        /**
         * 历史消耗原始额度。
         */
        @JsonProperty("historical_consumption")
        private Long historicalConsumption;

        /**
         * 剩余额度。
         */
        private Long quota;

        /**
         * 已用额度。
         */
        @JsonProperty("used_quota")
        private Long usedQuota;

        /**
         * 总额度。
         */
        @JsonProperty("total_quota")
        private Long totalQuota;

        /**
         * 当前余额金额。
         */
        @JsonProperty("current_balance_amount")
        private BigDecimal currentBalanceAmount;

        /**
         * 已用额度金额。
         */
        @JsonProperty("used_quota_amount")
        private BigDecimal usedQuotaAmount;

        /**
         * 总额度金额。
         */
        @JsonProperty("total_quota_amount")
        private BigDecimal totalQuotaAmount;

        /**
         * 额度金额换算比例。
         */
        @JsonProperty("quota_per_unit")
        private Long quotaPerUnit;
    }

    /**
     * new-api使用统计。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UsageStats {

        /**
         * 请求次数。
         */
        @JsonProperty("request_count")
        private Long requestCount;

        /**
         * 统计次数。
         */
        @JsonProperty("stat_count")
        private Long statCount;
    }

    /**
     * new-api资源消耗。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceConsumption {

        /**
         * 统计额度。
         */
        @JsonProperty("stat_quota")
        private Long statQuota;

        /**
         * 统计Token数。
         */
        @JsonProperty("stat_tokens")
        private Long statTokens;
    }

    /**
     * new-api性能指标。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PerformanceMetrics {

        /**
         * 平均RPM。
         */
        @JsonProperty("avg_rpm")
        private BigDecimal avgRpm;

        /**
         * 平均TPM。
         */
        @JsonProperty("avg_tpm")
        private BigDecimal avgTpm;
    }

    /**
     * new-api额度记录分页数据。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewApiQuotaRecordsData {

        /**
         * 页码。
         */
        private Integer page;

        /**
         * 每页数量。
         */
        @JsonProperty("page_size")
        private Integer pageSize;

        /**
         * 总数量。
         */
        private Long total;

        /**
         * 额度记录列表。
         */
        private List<QuotaRecordItem> items;
    }

    /**
     * new-api额度记录项。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuotaRecordItem {

        /**
         * 记录ID。
         */
        private Long id;

        /**
         * new-api用户ID。
         */
        @JsonProperty("user_id")
        private Long userId;

        /**
         * new-api用户名。
         */
        private String username;

        /**
         * 模型名称。
         */
        @JsonProperty("model_name")
        private String modelName;

        /**
         * 记录创建Unix时间戳秒。
         */
        @JsonProperty("created_at")
        private Long createdAt;

        /**
         * 消耗Token数。
         */
        @JsonProperty("token_used")
        private Long tokenUsed;

        /**
         * 请求次数。
         */
        private Long count;

        /**
         * 消耗额度。
         */
        private Long quota;
    }
}
