package com.zhidao.demo.service.impl;

import com.zhidao.demo.dto.AuditAiResult;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.AuditService;
import com.zhidao.demo.service.NotificationService;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.util.AiTextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AIService aiService;

    @Autowired
    private PostService postService;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void auditPost(Post post) {
        String title = post.getTitle() == null ? "" : post.getTitle();
        String content = AiTextUtils.stripImagesForAi(post.getContent());

        aiService.auditAndSummarize(title, content).subscribe((AuditAiResult r) -> {
            boolean approved = r != null && r.isApproved();
            if (approved) {
                post.setStatus("PUBLISHED");
                post.setAuditReason(null);
            } else {
                post.setStatus("AUDIT_PENDING");
                String reason = r == null ? null : r.getReason();
                post.setAuditReason(reason == null || reason.isBlank() ? "AI 审核未通过" : reason.trim());
            }

            if (r != null && r.getSummary() != null && !r.getSummary().isBlank()) {
                post.setAiSummary(r.getSummary().trim());
                post.setAiSummaryUpdateTime(java.time.LocalDateTime.now());
            }

            postService.updateById(post);

            if (approved) {
                notificationService.onPostAuditApproved(post);
            } else {
                notificationService.onPostAuditRejected(post, post.getAuditReason());
            }
        });
    }
}
