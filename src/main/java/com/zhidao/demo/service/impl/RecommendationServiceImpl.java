package com.zhidao.demo.service.impl;

import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.entity.UserAction;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.mapper.UserActionMapper;
import com.zhidao.demo.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserActionMapper userActionMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    public List<Post> recommendPosts(User user) {
        // Get all posts liked by the user
        List<UserAction> likedActions = userActionMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAction>()
                .eq(UserAction::getUserId, user.getId())
                .eq(UserAction::getType, UserAction.TYPE_LIKE_POST));

        List<Long> likedPostIds = likedActions.stream()
                .map(UserAction::getTargetId)
                .collect(Collectors.toList());

        if (likedPostIds.isEmpty()) {
            // If the user has not liked any posts, return the latest posts, excluding posts by the current user
            return postMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                    .ne(Post::getUserId, user.getId())
                    .orderByDesc(Post::getCreateTime)
                    .last("LIMIT 10"));
        } else {
            // Find posts with similar categories to the liked posts
            List<Post> likedPosts = postMapper.selectBatchIds(likedPostIds);
            List<Long> likedCategoryIds = likedPosts.stream().map(Post::getCategoryId).distinct().collect(Collectors.toList());

            // Find other posts in the same categories, excluding posts already liked and posts by the current user
            return postMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                    .in(Post::getCategoryId, likedCategoryIds)
                    .notIn(Post::getId, likedPostIds)
                    .ne(Post::getUserId, user.getId())
                    .orderByDesc(Post::getCreateTime)
                    .last("LIMIT 10"));
        }
    }
}
