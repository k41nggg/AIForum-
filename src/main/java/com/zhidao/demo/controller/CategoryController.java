package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.RecommendCategoryRequest;
import com.zhidao.demo.dto.RecommendCategoryResponse;
import com.zhidao.demo.entity.Category;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.Subscription;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.PostService;
import com.zhidao.demo.service.SubscriptionService;
import com.zhidao.demo.service.TopicClassificationService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TopicClassificationService topicClassificationService;

    @Autowired
    private PostService postService;

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
        if ("anonymousUser".equals(username)) return null;
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }

    // 1. 获取所有分类（树形）
    @GetMapping("/tree")
    public Result<List<Category>> listTree() {
        return Result.success(categoryService.list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)));
    }

    /**
     * 推荐分类：用于“发布帖子”页面编辑时调用。
     * 说明：这里不强制管理员；若 AI 判断需要新建分类，将按约定创建（仅父子关系）。
     */
    @PostMapping("/recommend")
    public Result<RecommendCategoryResponse> recommend(@RequestBody RecommendCategoryRequest req) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        if (req == null || req.getTitle() == null || req.getTitle().isBlank()
                || req.getContent() == null || req.getContent().isBlank()) {
            return Result.error("标题和内容不能为空");
        }

        RecommendCategoryResponse resp = topicClassificationService.recommendCategory(req.getTitle(), req.getContent());
        return Result.success(resp);
    }

    // 2. 创建分类（仅限管理员）
    @PostMapping
    public Result<Category> create(@RequestBody Category category) {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return Result.error("无权操作");
        }
        categoryService.save(category);
        return Result.success(category);
    }

    // 3. 编辑分类（仅限管理员）
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category category) {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return Result.error("无权操作");
        }
        category.setId(id);
        categoryService.updateById(category);
        return Result.success(category);
    }

    // 4. 删除分类（仅限管理员）
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return Result.error("无权操作");
        }

        // 规则1：如果分类下存在帖子，则禁止删除
        // 注意：当前 Post 实体未包含 isDeleted 字段，因此这里按分类维度统计即可。
        Long postCount = postService.count(new LambdaQueryWrapper<Post>()
                .eq(Post::getCategoryId, id));
        if (postCount != null && postCount > 0) {
            return Result.error("该分类下存在帖子，无法删除");
        }

        // 规则2：禁止删除有子分类的分类
        Long childrenCount = categoryService.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (childrenCount > 0) {
            return Result.error("该分类下存在子分类，无法删除，请先删除子分类");
        }

        categoryService.removeById(id);
        return Result.success(null);
    }

    // 5. 热门话题统计（按发帖量）
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> getHotCategories() {
        return Result.success(categoryService.getHotCategories());
    }

    // 6. 话题订阅功能
    @PostMapping("/{id}/subscribe")
    public Result<Void> subscribe(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setCategoryId(id);
        subscriptionService.save(sub);
        return Result.success(null);
    }

    // 7. 取消订阅
    @DeleteMapping("/{id}/unsubscribe")
    public Result<Void> unsubscribe(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        subscriptionService.remove(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId)
                .eq(Subscription::getCategoryId, id));
        return Result.success(null);
    }
}
