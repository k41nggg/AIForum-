package com.zhidao.demo.controller;

import com.zhidao.demo.common.Result;
import com.zhidao.demo.dto.LoginDTO;
import com.zhidao.demo.dto.RegisterDTO;
import com.zhidao.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    @PostMapping("/admin/register")
    public Result<String> registerAdmin(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.registerAdmin(registerDTO);
        return Result.success("管理员注册成功");
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        Map<String, String> map = new HashMap<>();
        map.put("token", token);
        return Result.success(map);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        // Since we're using JWT, logout on server is basically nothing unless we blacklist it.
        // Usually, client just deletes the token.
        return Result.success("登出成功");
    }
}
