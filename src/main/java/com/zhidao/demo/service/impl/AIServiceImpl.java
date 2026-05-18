package com.zhidao.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhidao.demo.dto.AuditAiResult;
import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
public class AIServiceImpl implements AIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIServiceImpl(WebClient.Builder webClientBuilder, @Value("${deepseek.api.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl("https://api.deepseek.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<ChatResponse> getCompletion(ChatRequest chatRequest) {
        return this.webClient.post()
                .uri("/v1/chat/completions")
                .body(Mono.just(chatRequest), ChatRequest.class)
                .retrieve()
                .bodyToMono(ChatResponse.class);
    }

    @Override
    public Mono<String> getModeration(String input) {
        // DeepSeek does not have a dedicated moderation endpoint.
        // We can use the chat completion endpoint to check for inappropriate content.
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(java.util.Collections.singletonList(
                new Message("user", "判断以下文本是否适合在公共论坛发布。如果适合，请只回答“是”。如果不适合，请回答“否”，并用中文简要说明理由，格式为“否：[原因]”。\n\n" + input)
        ));
        return getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

    @Override
    public Mono<AuditAiResult> auditAndSummarize(String title, String content) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是论坛内容审核与总结助手。请严格按 JSON 输出，不要输出任何多余文字。"),
                new Message("user",
                        "请完成两件事：\n" +
                                "1) 审核文本是否适合在公共论坛发布（垃圾广告、敏感内容、违规言论都算不适合）。\n" +
                                "2) 生成一句话中文总结（尽量不超过60字）。\n\n" +
                                "输出 JSON 格式如下：\n" +
                                "{\"approved\": true/false, \"reason\": \"不通过原因（通过则留空）\", \"summary\": \"一句话中文总结\"}\n\n" +
                                "标题：" + title + "\n\n正文：\n" + content)
        ));

        return getCompletion(chatRequest)
                .map(resp -> resp.getChoices().get(0).getMessage().getContent())
                .flatMap(text -> {
                    try {
                        AuditAiResult r = objectMapper.readValue(text, AuditAiResult.class);
                        // 兜底：避免 null
                        if (r.getReason() == null) r.setReason("");
                        if (r.getSummary() == null) r.setSummary("");
                        return Mono.just(r);
                    } catch (Exception e) {
                        // 解析失败时兜底：尽量不要让上层炸掉
                        AuditAiResult fallback = new AuditAiResult(false, "AI 返回非预期格式", "");
                        return Mono.just(fallback);
                    }
                });
    }
}
