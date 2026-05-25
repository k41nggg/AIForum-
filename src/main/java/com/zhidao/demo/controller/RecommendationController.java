package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.RecommendationResponse;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.RecommendationService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<RecommendationResponse> getRecommendations() {
        User user = requireUser();
        if (user == null) {
            return Result.error("未登录");
        }
        return Result.success(recommendationService.getRecommendations(user));
    }

    @PostMapping("/refresh")
    public Result<RecommendationResponse> refreshRecommendations() {
        User user = requireUser();
        if (user == null) {
            return Result.error("未登录");
        }
        return Result.success(recommendationService.refreshRecommendations(user));
    }

    private User requireUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            return u;
        }
        String username;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails details) {
            username = details.getUsername();
        } else if (principal instanceof String s && !"anonymousUser".equals(s)) {
            username = s;
        } else {
            return null;
        }
        return userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
