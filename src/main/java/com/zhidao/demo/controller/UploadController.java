package com.zhidao.demo.controller;

import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.UploadResponse;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.service.AttachmentService;
import com.zhidao.demo.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private UserService userService;

    @PostMapping
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        try {
            return Result.success(attachmentService.uploadImage(userId, file));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("上传失败");
        }
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
