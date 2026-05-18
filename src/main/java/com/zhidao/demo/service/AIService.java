package com.zhidao.demo.service;

import com.zhidao.demo.dto.AuditAiResult;
import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.ChatResponse;
import reactor.core.publisher.Mono;

public interface AIService {
    Mono<ChatResponse> getCompletion(ChatRequest chatRequest);

    Mono<String> getModeration(String input);

    /**
     * 审核内容是否适合发布，并同时生成一句话中文总结。
     * 目的：在一次模型调用中完成两件事，降低 token 与请求次数。
     */
    Mono<AuditAiResult> auditAndSummarize(String title, String content);
}
