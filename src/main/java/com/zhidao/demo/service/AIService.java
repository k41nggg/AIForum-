package com.zhidao.demo.service;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import reactor.core.publisher.Mono;

public interface AIService {
    Mono<ChatResponse> getCompletion(ChatRequest chatRequest);

    Mono<String> getModeration(String input);
}
