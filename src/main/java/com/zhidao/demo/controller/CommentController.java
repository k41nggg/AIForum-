package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.Comment;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.mapper.CommentMapper;
import com.zhidao.demo.service.CommentService;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentMapper commentMapper;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.zhidao.demo.entity.User) {
            return ((com.zhidao.demo.entity.User) principal).getId();
        }
        String username;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        if ("anonymousUser".equals(username)) {
            return null;
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }

    // 1. 发布评论/回复
    @PostMapping
    public Result<Comment> createComment(@RequestBody Comment comment) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        if (comment.getPostId() == null) return Result.error("帖子ID不能为空");
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        comment.setUserId(userId);
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setStatus(1); // 默认已发布

        // 处理回复逻辑 (parentId, rootId)
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            Comment parent = commentService.getById(comment.getParentId());
            if (parent != null) {
                // 如果父评论没有 rootId，说明父评论就是根评论
                comment.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
            }
        }

        commentService.save(comment);
        return Result.success(comment);
    }

    // 2. 根据帖子ID查询评论
    @GetMapping("/post/{postId}")
    public Result<List<Comment>> getCommentsByPost(@PathVariable Long postId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_comment.post_id", postId)
               .eq("forum_comment.status", 1)
               .eq("forum_comment.is_deleted", 0)
               .orderByDesc("forum_comment.create_time");
        return Result.success(commentMapper.selectListWithNickname(wrapper));
    }

    // 3. 删除评论
    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        if (comment == null) return Result.error("评论不存在");

        Long userId = getCurrentUserId();
        User currentUser = userService.getById(userId);

        // 作者或管理员可删除
        if (!comment.getUserId().equals(userId) && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("无权删除");
        }

        commentService.removeById(id);
        return Result.success(null);
    }

    // 4. 点赞
    @PostMapping("/{id}/like")
    public Result<Void> likeComment(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        if (comment == null) return Result.error("评论不存在");
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentService.updateById(comment);
        return Result.success(null);
    }
}
