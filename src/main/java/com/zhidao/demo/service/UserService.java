package com.zhidao.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.dto.LoginDTO;
import com.zhidao.demo.dto.RegisterDTO;

public interface UserService extends IService<User> {
    User getByUsername(String username);
    void register(RegisterDTO registerDTO);
    void registerAdmin(RegisterDTO registerDTO);
    String login(LoginDTO loginDTO);
    void updateUserProfile(Long userId, User profile);
    void updatePoints(Long userId, int points);
}
