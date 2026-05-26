package com.model.gateway.auth.websearch.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 网页搜索响应。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSearchResponse {

    /**
     * 搜索关键词。
     */
    private String query;

    /**
     * 消耗额度。
     */
    private Integer credits;

    /**
     * 搜索结果列表。
     */
    private List<WebSearchResultVo> results;
}
