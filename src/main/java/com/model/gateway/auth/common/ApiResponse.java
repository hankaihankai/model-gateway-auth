package com.model.gateway.auth.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口响应对象。
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    /**
     * 业务响应码。
     */
    private Integer code;

    /**
     * 业务响应消息。
     */
    private String message;

    /**
     * 业务响应数据。
     */
    private T data;

    /**
     * 构建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 构建失败响应。
     *
     * @param code 业务响应码
     * @param message 业务响应消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
