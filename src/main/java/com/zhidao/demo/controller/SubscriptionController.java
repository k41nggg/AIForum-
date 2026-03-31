package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.Category;
import com.zhidao.demo.entity.Subscription;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.SubscriptionService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

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

    public static class SubscribeDTO {
        private Long categoryId;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }
    }

    // 查询我的订阅（返回分类列表）
    @GetMapping("/me")
    public Result<List<Category>> mySubscriptions() {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        List<Subscription> subs = subscriptionService.list(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId));

        if (subs.isEmpty()) return Result.success(List.of());

        List<Long> categoryIds = subs.stream().map(Subscription::getCategoryId).distinct().collect(Collectors.toList());

        List<Category> categories = categoryService.listByIds(categoryIds);
        return Result.success(categories);
    }

    // 订阅分类（幂等：重复订阅提示“已订阅”）
    @PostMapping
    public Result<Void> subscribe(@RequestBody SubscribeDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        if (dto == null || dto.getCategoryId() == null) return Result.error("categoryId不能为空");

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setCategoryId(dto.getCategoryId());

        try {
            subscriptionService.save(sub);
        } catch (DuplicateKeyException ex) {
            // 唯一索引 uk_user_category(user_id, category_id) 防重
            return Result.error("已订阅");
        }

        return Result.success(null);
    }

    // 取消订阅（与前端统一：DELETE /api/subscriptions/{categoryId}
    @DeleteMapping("/{categoryId}")
    public Result<Void> unsubscribe(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        subscriptionService.remove(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId)
                .eq(Subscription::getCategoryId, categoryId));
        return Result.success(null);
    }
}
