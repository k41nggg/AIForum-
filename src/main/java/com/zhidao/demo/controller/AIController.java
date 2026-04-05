package com.zhidao.demo.controller;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.Message;
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
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是一个能干的助手，擅长总结论坛帖子。请使用中文回答。"),
                new Message("user", "请用一句话总结以下帖子内容：\n\n" + post.getContent())
        ));
        return aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }

    @PostMapping("/qa/{postId}")
    public Mono<String> getAnswer(@PathVariable Long postId, @RequestBody String question) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Mono.just("Post not found");
        }
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是一个能干的助手，可以根据提供的论坛帖子内容回答问题。请使用中文回答。"),
                new Message("user", "请根据以下帖子内容：\n\n" + post.getContent() + "\n\n回答以下问题：\n\n" + question)
        ));
        return aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent());
    }
}
