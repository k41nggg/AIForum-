package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.RecommendationService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.zhidao.demo.entity.User) {
            return ((com.zhidao.demo.entity.User) principal).getId();
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            return user != null ? user.getId() : null;
        }
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
             User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, (String)principal));
             return user != null ? user.getId() : null;
        }
        return null;
    }


    @GetMapping
    public Result<List<Post>> getRecommendations() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            // For anonymous users, return popular posts or an empty list
            return Result.success(Collections.emptyList());
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("User not found");
        }

        List<Post> recommendedPosts = recommendationService.recommendPosts(user);
        return Result.success(recommendedPosts);
    }
}
