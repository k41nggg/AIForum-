package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.UserSummaryVO;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.UserFollowService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-follows")
public class UserFollowController {

    @Autowired
    private UserFollowService userFollowService;

    @Autowired
    private UserService userService;

    public static class FollowDTO {
        private Long followeeId;

        public Long getFolloweeId() {
            return followeeId;
        }

        public void setFolloweeId(Long followeeId) {
            this.followeeId = followeeId;
        }
    }

    @GetMapping("/me")
    public Result<List<UserSummaryVO>> myFollowing() {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(userFollowService.listFollowing(userId));
    }

    @GetMapping("/check/{followeeId}")
    public Result<Map<String, Boolean>> check(@PathVariable Long followeeId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        boolean following = userFollowService.isFollowing(userId, followeeId);
        return Result.success(Map.of("following", following));
    }

    @GetMapping("/feed")
    public Result<List<Post>> feed(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(userFollowService.feedPosts(userId, limit));
    }

    @PostMapping
    public Result<Void> follow(@RequestBody FollowDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        if (dto == null || dto.getFolloweeId() == null) return Result.error("followeeId不能为空");
        try {
            userFollowService.follow(userId, dto.getFolloweeId());
            return Result.success(null);
        } catch (IllegalStateException e) {
            return Result.error("已关注");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{followeeId}")
    public Result<Void> unfollow(@PathVariable Long followeeId) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        userFollowService.unfollow(userId, followeeId);
        return Result.success(null);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        String username;
        if (principal instanceof UserDetails details) {
            username = details.getUsername();
        } else {
            username = principal.toString();
        }
        if ("anonymousUser".equals(username)) {
            return null;
        }
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
}
