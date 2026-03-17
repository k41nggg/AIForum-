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
