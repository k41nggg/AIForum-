package com.zhidao.demo.service;

import com.zhidao.demo.entity.Post;

public interface AuditService {
    boolean isContentAppropriate(String content);
    void auditPost(Post post);
}
