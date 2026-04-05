package com.zhidao.demo.service;

import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;

import java.util.List;

public interface RecommendationService {
    List<Post> recommendPosts(User user);
}
