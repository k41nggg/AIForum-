package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型请求调用的工具
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCall {
    private String id;
    private String type = "function";
    private FunctionCall function;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunctionCall {
        private String name;
        private String arguments; // JSON 字符串
    }
}