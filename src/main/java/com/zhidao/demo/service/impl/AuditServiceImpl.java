package com.zhidao.demo.service.impl;

import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.AuditService;
import com.zhidao.demo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AIService aiService;

    @Autowired
    private PostService postService;

    @Override
    public boolean isContentAppropriate(String content) {
        String moderationResult = aiService.getModeration(content).block();
        return moderationResult != null && !moderationResult.toLowerCase().contains("no");
    }

    @Override
    public void auditPost(Post post) {
        boolean isAppropriate = isContentAppropriate(post.getTitle() + " " + post.getContent());
        if (isAppropriate) {
            post.setStatus(2); // Approved
        } else {
            post.setStatus(3); // Rejected
        }
        postService.updateById(post);
    }
}
