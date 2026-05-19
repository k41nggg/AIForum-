package com.zhidao.demo.controller;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.dto.PostQaRequest;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.PostQaMemoryService;
import com.zhidao.demo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private PostQaMemoryService postQaMemoryService;

    @PostMapping("/summary/{postId}")
    public Mono<String> getSummary(@PathVariable Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Mono.just("Post not found");
        }

        // 1) 有缓存：直接返回数据库中的总结
        String summary = post.getAiSummary();
        if (summary != null && !summary.trim().isEmpty()) {
            return Mono.just(summary);
        }

        // 2) 无缓存：调用 AI 生成一次总结（尽量复用 auditAndSummarize，减少新增接口与 token 浪费）
        String title = post.getTitle() == null ? "" : post.getTitle();
        String content = post.getContent() == null ? "" : post.getContent();

        return aiService.auditAndSummarize(title, content)
                .map(r -> r == null ? "" : (r.getSummary() == null ? "" : r.getSummary().trim()))
                .defaultIfEmpty("")
                .flatMap(s -> {
                    if (s.isBlank()) {
                        return Mono.just("（AI 暂时无法生成总结，请稍后重试）");
                    }
                    post.setAiSummary(s);
                    post.setAiSummaryUpdateTime(java.time.LocalDateTime.now());
                    postService.updateById(post);
                    return Mono.just(s);
                });
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

        String title = post.getTitle() == null ? "" : post.getTitle();
        String content = post.getContent() == null ? "" : post.getContent();

        // 只对登录用户启用“单帖多轮（最近三轮）记忆”
        Long userId = getCurrentUserId();
        PostQaMemoryService.Memory memory = userId == null ? null : postQaMemoryService.get(postId, userId);

        String memoryBlock = "";
        if (memory != null && memory.history() != null && !memory.history().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n【最近三轮问答记忆】\n");
            int i = 1;
            for (PostQaMemoryService.QaPair qa : memory.history()) {
                if (qa == null) continue;
                String q = qa.question();
                String a = qa.answer();
                if (q == null || q.isBlank() || a == null || a.isBlank()) continue;
                sb.append(i).append(". 问：").append(q).append("\n");
                sb.append("   答：").append(a).append("\n");
                i++;
            }
            sb.append("（仅供参考，如与本次问题冲突请以帖子正文为准）\n");
            memoryBlock = sb.toString();
        }

        // 按需求：问答附带帖子正文 + 记忆（若有）作为上下文
        String prompt = "你正在阅读一篇论坛帖子，请基于帖子内容回答用户问题。\n" +
                "要求：用中文回答，简洁、有条理；如果帖子内容不足以回答，请说明信息不足。\n\n" +
                "【帖子标题】\n" + title + "\n\n" +
                "【帖子正文】\n" + content +
                memoryBlock + "\n" +
                "【本次问题】\n" + question.trim();

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(null);
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是一个能干的论坛助手，请只用中文回答。回答要简洁、有条理。"),
                new Message("user", prompt)
        ));

        return aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent())
                .doOnNext(answer -> {
                    if (userId != null && answer != null && !answer.isBlank()) {
                        postQaMemoryService.put(postId, userId, question.trim(), answer.trim());
                    }
                });
    }

    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                    ? null
                    : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User u) {
                return u.getId();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
