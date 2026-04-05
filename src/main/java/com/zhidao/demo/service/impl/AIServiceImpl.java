package com.zhidao.demo.service.impl;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import com.zhidao.demo.service.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AIServiceImpl implements AIService {

    private final WebClient webClient;

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
                new com.zhidao.demo.dto.Message("user", "以下文本是否适合在公共论坛上发布？请用“是”或“否”回答。\n\n" + input)
        ));
        return getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }
}
