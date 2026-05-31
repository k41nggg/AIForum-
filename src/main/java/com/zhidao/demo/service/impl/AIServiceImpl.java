package com.zhidao.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhidao.demo.config.AiProperties;
import com.zhidao.demo.dto.AuditAiResult;
import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.dto.ToolDefinition;
import com.zhidao.demo.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    private final WebClient webClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIServiceImpl(
            WebClient.Builder webClientBuilder,
            AiProperties aiProperties,
            @Value("${deepseek.api.key:}") String legacyApiKey) {
        this.aiProperties = aiProperties;
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = legacyApiKey;
            aiProperties.setApiKey(apiKey);
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 AI API Key，请在 application.yaml 设置 ai.api-key");
        }

        String baseUrl = aiProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.webClient = webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String defaultModel() {
        return aiProperties.getModel();
    }

    @Override
    public Mono<ChatResponse> getCompletion(ChatRequest chatRequest) {
        if (chatRequest.getModel() == null || chatRequest.getModel().isBlank()
                || "deepseek-coder".equals(chatRequest.getModel())) {
            chatRequest.setModel(defaultModel());
        }

        return this.webClient.post()
                .uri("/chat/completions")
                .body(Mono.just(chatRequest), ChatRequest.class)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException wex) {
                        log.error("硅基流动 API 调用失败: status={}, body={}",
                                wex.getStatusCode(), wex.getResponseBodyAsString());
                    } else {
                        log.error("硅基流动 API 调用失败", e);
                    }
                });
    }

    @Override
    public Mono<String> getModeration(String input) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(defaultModel());
        chatRequest.setMessages(java.util.Collections.singletonList(
                new Message("user", "判断以下文本是否适合在公共论坛发布。如果适合，请只回答“是”。如果不适合，请回答“否”，并用中文简要说明理由，格式为“否：[原因]”。\n\n" + input)
        ));
        return getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

    @Override
    public Mono<AuditAiResult> auditAndSummarize(String title, String content) {
        // 使用 Tool Calling 方式
        List<Message> messages = Arrays.asList(
                new Message("system", "你是论坛内容审核助手，请使用 audit_post 工具完成审核与总结。"),
                new Message("user", "标题：" + (title == null ? "" : title) + "\n\n正文：\n" + (content == null ? "" : content))
        );

        return getCompletionWithTools(messages, List.of(com.zhidao.demo.util.AiTools.auditPostTool()))
                .map(resp -> {
                    Message msg = resp.getChoices().get(0).getMessage();
                    if (msg.getTool_calls() != null && !msg.getTool_calls().isEmpty()) {
                        String args = msg.getTool_calls().get(0).getFunction().getArguments();
                        try {
                            String json = stripMarkdownCodeFence(args);
                            AuditAiResult r = objectMapper.readValue(json, AuditAiResult.class);
                            if (r.getReason() == null) r.setReason("");
                            if (r.getSummary() == null) r.setSummary("");
                            return r;
                        } catch (Exception e) {
                            log.warn("Tool Calling 审核结果解析失败, raw={}", args);
                        }
                    }
                    // fallback
                    return new AuditAiResult(false, "AI 未返回有效工具调用", "");
                });
    }

    @Override
    public Mono<ChatResponse> getCompletionWithTools(List<Message> messages, List<ToolDefinition> tools) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(defaultModel());
        chatRequest.setMessages(messages);
        chatRequest.setTools(tools);
        chatRequest.setTool_choice("auto");
        return getCompletion(chatRequest);
    }

    private static String stripMarkdownCodeFence(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
