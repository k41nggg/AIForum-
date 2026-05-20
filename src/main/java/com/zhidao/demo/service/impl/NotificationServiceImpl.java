package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.constant.NotificationType;
import com.zhidao.demo.entity.*;
import com.zhidao.demo.mapper.SysMessageMapper;
import com.zhidao.demo.mapper.UserFollowMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.CommentService;
import com.zhidao.demo.service.NotificationService;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private SysMessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Override
    public void onPostAuditApproved(Post post) {
        if (post == null || post.getId() == null) return;
        String title = truncate(post.getTitle(), 40);
        send(post.getUserId(), null, NotificationType.POST_AUDIT_APPROVED,
                "帖子审核通过",
                "你的帖子《" + title + "》已通过审核并发布。",
                "post", post.getId(), null);
        broadcastNewPost(post);
    }

    @Override
    public void onPostAuditRejected(Post post, String reason) {
        if (post == null || post.getId() == null) return;
        String title = truncate(post.getTitle(), 40);
        String r = (reason == null || reason.isBlank()) ? "未提供具体原因" : reason.trim();
        send(post.getUserId(), null, NotificationType.POST_AUDIT_REJECTED,
                "帖子审核未通过",
                "你的帖子《" + title + "》未通过审核。原因：" + r,
                "post", post.getId(), null);
    }

    @Override
    public void onPostRemovedByAdmin(Post post, Long adminId) {
        if (post == null || post.getId() == null) return;
        if (post.getUserId().equals(adminId)) return;
        String title = truncate(post.getTitle(), 40);
        send(post.getUserId(), adminId, NotificationType.POST_REMOVED,
                "帖子已被下架",
                "你的帖子《" + title + "》已被管理员下架或删除。",
                "post", post.getId(), null);
    }

    @Override
    public void onPostCommented(Comment comment, Post post) {
        if (comment == null || post == null) return;
        if (comment.getUserId().equals(post.getUserId())) return;
        String actor = displayName(comment.getUserId());
        String pTitle = truncate(post.getTitle(), 30);
        send(post.getUserId(), comment.getUserId(), NotificationType.POST_COMMENTED,
                "收到新评论",
                actor + " 评论了你的帖子《" + pTitle + "》",
                "post", post.getId(), comment.getId());
    }

    @Override
    public void onCommentReplied(Comment comment, Comment parent, Post post) {
        if (comment == null || parent == null || post == null) return;
        if (comment.getUserId().equals(parent.getUserId())) return;
        String actor = displayName(comment.getUserId());
        String pTitle = truncate(post.getTitle(), 30);
        send(parent.getUserId(), comment.getUserId(), NotificationType.COMMENT_REPLIED,
                "收到回复",
                actor + " 回复了你在《" + pTitle + "》下的评论",
                "comment", comment.getId(), post.getId());
    }

    @Override
    public void onPostLiked(Long postId, Long likerId) {
        Post post = postService.getById(postId);
        if (post == null || likerId == null) return;
        if (likerId.equals(post.getUserId())) return;
        if (isDuplicateLikeNotify(post.getUserId(), likerId, NotificationType.POST_LIKED, postId)) return;
        String actor = displayName(likerId);
        String pTitle = truncate(post.getTitle(), 30);
        send(post.getUserId(), likerId, NotificationType.POST_LIKED,
                "帖子被点赞",
                actor + " 赞了你的帖子《" + pTitle + "》",
                "post", postId, null);
    }

    @Override
    public void onCommentLiked(Long commentId, Long likerId) {
        Comment comment = commentService.getById(commentId);
        if (comment == null || likerId == null) return;
        if (likerId.equals(comment.getUserId())) return;
        if (isDuplicateLikeNotify(comment.getUserId(), likerId, NotificationType.COMMENT_LIKED, commentId)) return;
        String actor = displayName(likerId);
        send(comment.getUserId(), likerId, NotificationType.COMMENT_LIKED,
                "评论被点赞",
                actor + " 赞了你的评论",
                "comment", commentId, comment.getPostId());
    }

    @Override
    public void onUserFollowed(Long followeeId, Long followerId) {
        if (followeeId == null || followerId == null || followeeId.equals(followerId)) return;
        String actor = displayName(followerId);
        send(followeeId, followerId, NotificationType.USER_FOLLOWED,
                "新增粉丝",
                actor + " 关注了你",
                "user", followerId, null);
    }

    private void broadcastNewPost(Post post) {
        if (post == null || post.getUserId() == null) return;
        String author = displayName(post.getUserId());
        String pTitle = truncate(post.getTitle(), 40);

        List<UserFollow> follows = userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, post.getUserId()));
        for (UserFollow f : follows) {
            if (f.getFollowerId().equals(post.getUserId())) continue;
            send(f.getFollowerId(), post.getUserId(), NotificationType.FOLLOWEE_NEW_POST,
                    "关注的人发帖",
                    "你关注的 " + author + " 发布了《" + pTitle + "》",
                    "post", post.getId(), null);
        }

        if (post.getCategoryId() != null) {
            Category cat = categoryService.getById(post.getCategoryId());
            String catName = cat != null ? cat.getName() : "该分类";
            List<Subscription> subs = subscriptionService.list(new LambdaQueryWrapper<Subscription>()
                    .eq(Subscription::getCategoryId, post.getCategoryId()));
            for (Subscription s : subs) {
                if (s.getUserId().equals(post.getUserId())) continue;
                send(s.getUserId(), post.getUserId(), NotificationType.CATEGORY_NEW_POST,
                        "订阅分类有新帖",
                        "你订阅的【" + catName + "】有新帖《" + pTitle + "》",
                        "post", post.getId(), post.getCategoryId());
            }
        }
    }

    private void send(Long receiverId, Long senderId, int type, String title, String content,
                      String targetType, Long targetId, Long extraId) {
        if (receiverId == null) return;
        SysMessage msg = new SysMessage();
        msg.setReceiverId(receiverId);
        msg.setSenderId(senderId);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setTargetType(targetType);
        msg.setTargetId(targetId);
        msg.setExtraId(extraId);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    private boolean isDuplicateLikeNotify(Long receiverId, Long senderId, int type, Long targetId) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getSenderId, senderId)
                .eq(SysMessage::getType, type)
                .eq(SysMessage::getTargetId, targetId)
                .ge(SysMessage::getCreateTime, since)) > 0;
    }

    private String displayName(Long userId) {
        if (userId == null) return "某用户";
        User u = userMapper.selectById(userId);
        if (u == null) return "某用户";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname().trim();
        return u.getUsername() != null ? u.getUsername() : "某用户";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
