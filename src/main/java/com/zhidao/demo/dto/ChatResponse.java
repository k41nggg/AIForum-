package com.zhidao.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
        private FinishReason finish_reason;
    }

    public enum FinishReason {
        stop, length, content_filter, tool_calls
    }
}
