package com.zhidao.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.MessageVO;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.SysMessageService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private SysMessageService messageService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<IPage<MessageVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(messageService.listMessages(userId, current, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        return Result.success(Map.of("count", messageService.countUnread(userId)));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        messageService.markRead(userId, id);
        return Result.success(null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        messageService.markAllRead(userId);
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
