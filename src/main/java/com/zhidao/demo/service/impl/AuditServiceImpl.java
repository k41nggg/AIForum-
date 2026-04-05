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
    public void auditPost(Post post) {
        String contentToAudit = post.getTitle() + "\n" + post.getContent();
        aiService.getModeration(contentToAudit).subscribe(moderationResult -> {
            if (moderationResult != null && moderationResult.startsWith("是")) {
                post.setStatus("PUBLISHED");
                post.setAuditReason(null);
            } else {
                post.setStatus("AUDIT_PENDING");
                if (moderationResult != null && moderationResult.startsWith("否")) {
                    post.setAuditReason(moderationResult.substring(moderationResult.indexOf("：") + 1).trim());
                } else {
                    post.setAuditReason("AI service returned an unexpected response.");
                }
            }
            postService.updateById(post);
        });
    }
}
