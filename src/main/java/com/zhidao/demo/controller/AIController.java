package com.zhidao.demo.controller;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.dto.PostQaRequest;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private PostService postService;

    @PostMapping("/summary/{postId}")
    public Mono<String> getSummary(@PathVariable Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Mono.just("Post not found");
        }

        // 按要求：总结只返回审核阶段生成并落库的 summary，不再调用 AI
        String summary = post.getAiSummary();
        if (summary == null || summary.trim().isEmpty()) {
            return Mono.just("（该帖子尚未生成 AI 总结，请等待审核完成后再试）");
        }
        return Mono.just(summary);
    }

    @PostMapping("/qa/{postId}")
    public Mono<String> getAnswer(@PathVariable Long postId, @RequestBody PostQaRequest body) {
        // 仍然校验帖子存在（避免对不存在帖子无限提问）
        Post post = postService.getById(postId);
        if (post == null) {
            return Mono.just("Post not found");
        }

        String question = body == null ? null : body.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Mono.just("问题不能为空");
        }

        // 按要求：问答不附带正文和总结，只发送问题
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是一个能干的助手，请用中文回答用户问题。回答要简洁、有条理。"),
                new Message("user", question.trim())
        ));

        return aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }
}
