package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String model;
    private List<Message> messages;

    /** Tool Calling 支持：工具定义列表 */
    private List<ToolDefinition> tools;

    /** Tool Calling 支持：工具选择策略 */
    private Object tool_choice;
}
