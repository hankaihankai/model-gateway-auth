package com.model.gateway.auth.websearch.interfaces.http;

import com.model.gateway.auth.shared.api.ApiResponse;
import com.model.gateway.auth.websearch.application.WebSearchApplicationService;
import com.model.gateway.auth.websearch.interfaces.vo.WebSearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网页搜索接口控制器。
 */
@RestController
@RequestMapping("/api/websearch")
public class WebSearchController {

    /**
     * 网页搜索应用服务。
     */
    private final WebSearchApplicationService webSearchApplicationService;

    /**
     * 创建网页搜索接口控制器。
     *
     * @param webSearchApplicationService 网页搜索应用服务
     */
    public WebSearchController(WebSearchApplicationService webSearchApplicationService) {
        this.webSearchApplicationService = webSearchApplicationService;
    }

    /**
     * 查询网页搜索结果。
     *
     * @param q 搜索关键词
     * @param num 返回结果数量
     * @param gl 搜索地区
     * @param hl 搜索语言
     * @return 网页搜索结果
     */
    @GetMapping("/search")
    public ApiResponse<WebSearchResponse> search(
            @RequestParam("q") String q,
            @RequestParam(value = "num", required = false) Integer num,
            @RequestParam(value = "gl", required = false) String gl,
            @RequestParam(value = "hl", required = false) String hl) {
        return ApiResponse.success(webSearchApplicationService.search(q, num, gl, hl));
    }
}
