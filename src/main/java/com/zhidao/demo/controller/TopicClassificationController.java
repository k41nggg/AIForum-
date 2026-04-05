package com.zhidao.demo.controller;

import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.service.TopicClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classify")
public class TopicClassificationController {

    @Autowired
    private TopicClassificationService topicClassificationService;

    @Autowired
    private PostService postService;

    @PostMapping("/{postId}")
    public Result<Long> classifyPost(@PathVariable Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error("Post not found");
        }
        Long categoryId = topicClassificationService.classifyPost(post);
        if (categoryId != null) {
            post.setCategoryId(categoryId);
            postService.updateById(post);
        }
        return Result.success(categoryId);
    }
}
