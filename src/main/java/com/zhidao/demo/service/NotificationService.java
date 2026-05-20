package com.zhidao.demo.service;

import com.zhidao.demo.entity.Comment;
import com.zhidao.demo.entity.Post;

public interface NotificationService {

    void onPostAuditApproved(Post post);

    void onPostAuditRejected(Post post, String reason);

    void onPostRemovedByAdmin(Post post, Long adminId);

    void onPostCommented(Comment comment, Post post);

    void onCommentReplied(Comment comment, Comment parent, Post post);

    void onPostLiked(Long postId, Long likerId);

    void onCommentLiked(Long commentId, Long likerId);

    void onUserFollowed(Long followeeId, Long followerId);
}
