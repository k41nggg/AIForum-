package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    private String role;
    private String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /** Tool Calling：当 role=assistant 时，模型请求调用哪些工具 */
    private List<ToolCall> tool_calls;

    /** Tool Calling：当 role=tool 时，指定对应 tool_call 的 id */
    private String tool_call_id;

    /** Tool Calling：tool 调用时使用的函数名（便于日志与调试） */
    private String name;
}
