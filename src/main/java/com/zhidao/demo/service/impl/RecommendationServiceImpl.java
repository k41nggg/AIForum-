package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.entity.UserAction;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.mapper.UserActionMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserActionMapper userActionMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

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
            QueryWrapper<Post> wrapper = publishedWrapper();
            wrapper.ne("forum_post.user_id", user.getId())
                    .orderByDesc("forum_post.create_time")
                    .last("LIMIT 10");
            return enrichAuthorProfile(postMapper.selectListWithNickname(wrapper));
        }

        List<Post> likedPosts = postMapper.selectBatchIds(likedPostIds);
        List<Long> likedCategoryIds = likedPosts.stream().map(Post::getCategoryId).distinct().collect(Collectors.toList());
        if (likedCategoryIds.isEmpty()) {
            QueryWrapper<Post> wrapper = publishedWrapper();
            wrapper.ne("forum_post.user_id", user.getId())
                    .orderByDesc("forum_post.create_time")
                    .last("LIMIT 10");
            return enrichAuthorProfile(postMapper.selectListWithNickname(wrapper));
        }

        QueryWrapper<Post> wrapper = publishedWrapper();
        wrapper.in("forum_post.category_id", likedCategoryIds)
                .notIn("forum_post.id", likedPostIds)
                .ne("forum_post.user_id", user.getId())
                .orderByDesc("forum_post.create_time")
                .last("LIMIT 10");
        return enrichAuthorProfile(postMapper.selectListWithNickname(wrapper));
    }

    /** 补齐作者昵称/头像（与帖子广场展示一致） */
    private List<Post> enrichAuthorProfile(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return posts == null ? Collections.emptyList() : posts;
        }
        List<Long> userIds = posts.stream().map(Post::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (Post post : posts) {
            User author = userMap.get(post.getUserId());
            if (author == null) continue;
            if (post.getUserNickname() == null || post.getUserNickname().isBlank()) {
                post.setUserNickname(author.getNickname());
            }
            if (post.getUserAvatar() == null || post.getUserAvatar().isBlank()) {
                post.setUserAvatar(author.getAvatar());
            }
        }
        return posts;
    }

    private static QueryWrapper<Post> publishedWrapper() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_post.status", "PUBLISHED");
        wrapper.eq("forum_post.is_deleted", 0);
        return wrapper;
    }
}
