package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.entity.UserAction;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.service.AuditService;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.service.TopicClassificationService;
import com.zhidao.demo.service.UserActionService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserActionService userActionService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TopicClassificationService topicClassificationService;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 如果是我们的 User 实体（JwtAuthenticationFilter 中存入的）
        if (principal instanceof com.zhidao.demo.entity.User) {
            return ((com.zhidao.demo.entity.User) principal).getId();
        }

        // fallback: 如果是 Security 默认的 UserDetails 或 字符串
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

    // 1. 发布帖子
    @PostMapping
    public Result<Post> createPost(@RequestBody Post post) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        post.setUserId(userId);
        post.setStatus("AUDIT_PENDING"); // 初始状态为待审核
        postService.save(post);

        // 异步执行AI审核和分类
        auditService.auditPost(post);
        topicClassificationService.classifyPost(post);

        return Result.success(post);
    }

    @PutMapping("/{id}")
    public Result<Post> updatePost(@PathVariable Long id, @RequestBody Post updatedPost) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        Post existingPost = postService.getById(id);
        if (existingPost == null) return Result.error("帖子不存在");
        if (!existingPost.getUserId().equals(userId)) {
            return Result.error("无权修改他人帖子");
        }

        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());
        existingPost.setCategoryId(updatedPost.getCategoryId());
        existingPost.setStatus("AUDIT_PENDING"); // 每次编辑后重新进入待审核
        postService.updateById(existingPost);

        // 异步执行AI审核和分类
        auditService.auditPost(existingPost);
        if (updatedPost.getCategoryId() == null) {
            topicClassificationService.classifyPost(existingPost);
        }

        return Result.success(existingPost);
    }

    // 3. 删除帖子
    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Post oldPost = postService.getById(id);
        if (oldPost == null) return Result.error("帖子不存在");

        Long userId = getCurrentUserId();
        User currentUser = userService.getById(userId);

        // 作者或管理员可删除
        if (!oldPost.getUserId().equals(userId) && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("无权删除");
        }

        postService.removeById(id);
        return Result.success(null);
    }

    // 4. 下架帖子 (改变状态)
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Post oldPost = postService.getById(id);
        if (oldPost == null) return Result.error("帖子不存在");

        Long userId = getCurrentUserId();
        User currentUser = userService.getById(userId);

        // 作者可操作草稿/下架，管理员可操作任何状态
        if (!oldPost.getUserId().equals(userId) && !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("无权操作");
        }

        oldPost.setStatus(status);
        postService.updateById(oldPost);
        return Result.success(null);
    }

    // 5. 帖子查询（分页、排序、筛选）
    @GetMapping
    public Result<IPage<Post>> listPosts(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {

        IPage<Post> page = new Page<>(current, size);
        QueryWrapper<Post> wrapper = new QueryWrapper<>();

        // 明确指定表名防止 Ambiguous column 错误
        wrapper.eq("forum_post.status", "PUBLISHED");
        wrapper.eq("forum_post.is_deleted", 0);

        if (categoryId != null) wrapper.eq("forum_post.category_id", categoryId);
        if (keyword != null) {
            wrapper.and(w -> w.like("forum_post.title", keyword).or().like("forum_post.content", keyword));
        }

        // 排序
        if ("createTime".equals(sortBy)) {
            if ("asc".equals(order)) wrapper.orderByAsc("forum_post.create_time");
            else wrapper.orderByDesc("forum_post.create_time");
        } else if ("viewCount".equals(sortBy)) {
            wrapper.orderByDesc("forum_post.view_count");
        } else if ("likeCount".equals(sortBy)) {
            wrapper.orderByDesc("forum_post.like_count");
        }

        return Result.success(postMapper.selectPageWithNickname(page, wrapper));
    }

    // 6. 帖子详情与浏览量统计
    @GetMapping("/{id}")
    public Result<Post> getPostDetail(@PathVariable Long id) {
        Post post = postMapper.selectByIdWithNickname(id);
        if (post == null) return Result.error("帖子不存在");

        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postService.updateById(post);

        return Result.success(post);
    }

    // 7. 点赞（同一用户对同一帖子只能点赞一次）
    @PostMapping("/{id}/like")
    @Transactional
    public Result<Void> likePost(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        Post post = postService.getById(id);
        if (post == null) return Result.error("帖子不存在");

        UserAction action = new UserAction();
        action.setUserId(userId);
        action.setTargetId(id);
        action.setType(UserAction.TYPE_LIKE_POST);

        try {
            // 依赖 forum_user_action 的唯一索引 uk_user_target_type 去重
            userActionService.save(action);
        } catch (DuplicateKeyException ex) {
            // 重复点赞：幂等返回，不再增加计数
            return Result.error("已点赞");
        }

        // 只有首次点赞才会自增
        postMapper.incLikeCount(id);

        return Result.success(null);
    }

    @PostMapping("/{id}/collect")
    public Result<Void> collectPost(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post == null) return Result.error("帖子不存在");
        post.setCollectCount(post.getCollectCount() + 1);
        postService.updateById(post);
        return Result.success(null);
    }

    // 8. 帖子举报处理 (模拟接口)
    @PostMapping("/{id}/report")
    public Result<Void> reportPost(@PathVariable Long id, @RequestBody Map<String, String> reportData) {
        // 这里可以存入举报表，目前仅作为功能点展示
        return Result.success(null);
    }

    // 9. 管理员：获取待审核帖子列表
    @GetMapping("/audit/list")
    public Result<IPage<Post>> listAuditPosts(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        User currentUser = userService.getById(userId);
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("无权访问");
        }

        IPage<Post> page = new Page<>(current, size);
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_post.status", "AUDIT_PENDING"); // "AUDIT_PENDING"
        wrapper.eq("forum_post.is_deleted", 0);
        wrapper.orderByDesc("forum_post.create_time");

        return Result.success(postMapper.selectPageWithNickname(page, wrapper));
    }

    // 10. 管理员：审核帖子
    @PutMapping("/{id}/audit")
    public Result<Void> auditPost(@PathVariable Long id, @RequestParam String status) {
        // status: "PUBLISHED", "DELETED"
        if (!"PUBLISHED".equals(status) && !"DELETED".equals(status)) {
            return Result.error("非法状态");
        }

        Long userId = getCurrentUserId();
        User currentUser = userService.getById(userId);
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            return Result.error("无权操作");
        }

        Post post = postService.getById(id);
        if (post == null) return Result.error("帖子不存在");

        post.setStatus(status);
        postService.updateById(post);
        return Result.success(null);
    }
}
