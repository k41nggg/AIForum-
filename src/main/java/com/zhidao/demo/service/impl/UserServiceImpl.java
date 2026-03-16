package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.service.UserService;
import com.zhidao.demo.dto.LoginDTO;
import com.zhidao.demo.dto.RegisterDTO;
import com.zhidao.demo.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        registerBase(registerDTO, "USER");
    }

    @Override
    @Transactional
    public void registerAdmin(RegisterDTO registerDTO) {
        registerBase(registerDTO, "ADMIN");
    }

    private void registerBase(RegisterDTO registerDTO, String role) {
        if (getByUsername(registerDTO.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(registerDTO.getNickname());
        user.setRole(role);
        user.setStatus(1);
        user.setPoints(0);
        user.setLevel(1);
        save(user);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        User user = getByUsername(loginDTO.getUsername());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        return jwtUtils.generateToken(user.getUsername());
    }

    @Override
    @Transactional
    public void updateUserProfile(Long userId, User profile) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setNickname(profile.getNickname());
        user.setAvatar(profile.getAvatar());
        user.setBio(profile.getBio());
        updateById(user);
    }

    @Override
    @Transactional
    public void updatePoints(Long userId, int points) {
        User user = getById(userId);
        if (user != null) {
            user.setPoints(user.getPoints() + points);
            // Simple level system: 100 points per level
            user.setLevel(user.getPoints() / 100 + 1);
            updateById(user);
        }
    }
}
