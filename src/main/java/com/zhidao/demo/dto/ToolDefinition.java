package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * OpenAI 兼容的 Tool 定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolDefinition {
    private String type = "function";
    private FunctionDefinition function;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}