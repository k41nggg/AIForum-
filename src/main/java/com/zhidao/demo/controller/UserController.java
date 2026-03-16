package com.zhidao.demo.controller;

import com.zhidao.demo.common.Result;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public Result<User> getCurrentUser(@AuthenticationPrincipal User user) {
        // user is injected by JwtAuthenticationFilter
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@AuthenticationPrincipal User user, @RequestBody User profile) {
        userService.updateUserProfile(user.getId(), profile);
        return Result.success("更新成功");
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null); // Clear password before returning
        }
        return Result.success(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }
}
