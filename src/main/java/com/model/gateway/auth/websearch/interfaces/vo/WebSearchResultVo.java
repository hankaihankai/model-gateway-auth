package com.model.gateway.auth.websearch.interfaces.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网页搜索结果项。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSearchResultVo {

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
