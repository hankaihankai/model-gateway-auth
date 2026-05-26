package com.model.gateway.auth.websearch.application;

import com.model.gateway.auth.shared.exception.AuthException;
import com.model.gateway.auth.websearch.infrastructure.external.SerperWebSearchAcl;
import com.model.gateway.auth.websearch.interfaces.vo.WebSearchResponse;
import com.model.gateway.auth.websearch.interfaces.vo.WebSearchResultVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 网页搜索应用服务。
 */
@Service
public class WebSearchApplicationService {

    /**
     * 默认搜索结果数量。
     */
    private static final int DEFAULT_NUM = 10;

    /**
     * 最大搜索结果数量。
     */
    private static final int MAX_NUM = 20;

    /**
     * Serper外部搜索接口ACL。
     */
    private final SerperWebSearchAcl serperWebSearchAcl;

    /**
     * 创建网页搜索应用服务。
     *
     * @param serperWebSearchAcl Serper外部搜索接口ACL
     */
    public WebSearchApplicationService(SerperWebSearchAcl serperWebSearchAcl) {
        this.serperWebSearchAcl = serperWebSearchAcl;
    }

    /**
     * 执行网页搜索。
     *
     * @param query 搜索关键词
     * @param num 返回结果数量
     * @param gl 搜索地区
     * @param hl 搜索语言
     * @return 网页搜索响应
     */
    public WebSearchResponse search(String query, Integer num, String gl, String hl) {
        if (!StringUtils.hasText(query)) {
            throw new AuthException("搜索关键词不能为空");
        }
        SerperWebSearchAcl.SerperSearchResponse response = serperWebSearchAcl.search(
                SerperWebSearchAcl.SerperSearchRequest.builder()
                        .q(query.trim())
                        .num(resolveNum(num))
                        .gl(trimToNull(gl))
                        .hl(trimToNull(hl))
                        .build()
        );
        return toWebSearchResponse(query.trim(), response);
    }

    /**
     * 转换网页搜索响应。
     *
     * @param query 搜索关键词
     * @param response Serper搜索响应
     * @return 网页搜索响应
     */
    private WebSearchResponse toWebSearchResponse(String query, SerperWebSearchAcl.SerperSearchResponse response) {
        if (response == null) {
            throw new AuthException("Serper搜索响应为空");
        }
        List<WebSearchResultVo> results = response.getOrganic() == null ? Collections.emptyList() : response.getOrganic()
                .stream()
                .map(item -> WebSearchResultVo.builder()
                        .title(item.getTitle())
                        .link(item.getLink())
                        .snippet(item.getSnippet())
                        .position(item.getPosition())
                        .build())
                .toList();
        String responseQuery = response.getSearchParameters() == null ? query : response.getSearchParameters().getQ();
        return WebSearchResponse.builder()
                .query(StringUtils.hasText(responseQuery) ? responseQuery : query)
                .credits(response.getCredits())
                .results(results)
                .build();
    }

    /**
     * 解析搜索结果数量。
     *
     * @param num 请求结果数量
     * @return 合法结果数量
     */
    private Integer resolveNum(Integer num) {
        if (num == null) {
            return DEFAULT_NUM;
        }
        if (num < 1 || num > MAX_NUM) {
            throw new AuthException("搜索结果数量应在1到20之间");
        }
        return num;
    }

    /**
     * 清理可选字符串。
     *
     * @param value 原始字符串
     * @return 非空字符串或null
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
