package com.zhidao.demo.service.impl;

import com.zhidao.demo.dto.AuditAiResult;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.AuditService;
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

    @Override
    public void auditPost(Post post) {
        String title = post.getTitle() == null ? "" : post.getTitle();
        String content = AiTextUtils.stripImagesForAi(post.getContent());

        aiService.auditAndSummarize(title, content).subscribe((AuditAiResult r) -> {
            if (r != null && r.isApproved()) {
                post.setStatus("PUBLISHED");
                post.setAuditReason(null);
            } else {
                post.setStatus("AUDIT_PENDING");
                String reason = r == null ? null : r.getReason();
                post.setAuditReason(reason == null || reason.isBlank() ? "AI 审核未通过" : reason.trim());
            }

            // 无论通过与否，都尽量写入 summary（便于前端展示，也避免后续重复调用）
            if (r != null && r.getSummary() != null && !r.getSummary().isBlank()) {
                post.setAiSummary(r.getSummary().trim());
                post.setAiSummaryUpdateTime(java.time.LocalDateTime.now());
            }

            postService.updateById(post);
        });
    }
}
