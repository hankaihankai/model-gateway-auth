package com.model.gateway.auth.websearch.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.model.gateway.auth.shared.exception.AuthException;
import com.model.gateway.auth.websearch.infrastructure.config.SerperProperties;
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

import java.time.Duration;
import java.util.List;

/**
 * Serper外部搜索接口ACL。
 */
@Component
public class SerperWebSearchAcl {

    /**
     * Serper接口密钥请求头。
     */
    private static final String API_KEY_HEADER = "X-API-KEY";

    /**
     * Serper搜索接口配置。
     */
    private final SerperProperties properties;

    /**
     * REST客户端。
     */
    private final RestClient restClient;

    /**
     * 创建Serper外部搜索接口ACL。
     *
     * @param properties Serper搜索接口配置
     * @param restClientBuilder REST客户端构建器
     */
    public SerperWebSearchAcl(SerperProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .requestFactory(buildRequestFactory(properties.getTimeoutMillis()))
                .build();
    }

    /**
     * 执行Serper网页搜索。
     *
     * @param request Serper搜索请求
     * @return Serper搜索响应
     */
    public SerperSearchResponse search(SerperSearchRequest request) {
        checkConfig();
        try {
            return restClient.post()
                    .uri(properties.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<SerperSearchResponse>() {
                    });
        } catch (RestClientResponseException exception) {
            throw new AuthException("Serper搜索失败: HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new AuthException("Serper搜索失败: " + exception.getMessage());
        }
    }

    /**
     * 校验Serper接口配置。
     */
    private void checkConfig() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AuthException("Serper API Key未配置");
        }
        if (!StringUtils.hasText(properties.getEndpoint())) {
            throw new AuthException("Serper搜索接口地址未配置");
        }
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
     * Serper搜索请求。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SerperSearchRequest {

        /**
         * 搜索关键词。
         */
        private String q;

        /**
         * 返回结果数量。
         */
        private Integer num;

        /**
         * 搜索地区。
         */
        private String gl;

        /**
         * 搜索语言。
         */
        private String hl;
    }

    /**
     * Serper搜索响应。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SerperSearchResponse {

        /**
         * 搜索关键词。
         */
        @JsonProperty("searchParameters")
        private SearchParameters searchParameters;

        /**
         * 搜索结果列表。
         */
        private List<OrganicItem> organic;

        /**
         * 消耗额度。
         */
        private Integer credits;
    }

    /**
     * Serper搜索参数。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchParameters {

        /**
         * 搜索关键词。
         */
        private String q;
    }

    /**
     * Serper自然搜索结果项。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrganicItem {

        /**
         * 结果标题。
         */
        private String title;

        /**
         * 结果链接。
         */
        private String link;

        /**
         * 结果摘要。
         */
        private String snippet;

        /**
         * 结果排序位置。
         */
        private Integer position;
    }
}
