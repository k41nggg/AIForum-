package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
            QueryWrapper<Post> wrapper = publishedWrapper();
            wrapper.ne("forum_post.user_id", user.getId())
                    .orderByDesc("forum_post.create_time")
                    .last("LIMIT 10");
            return postMapper.selectListWithNickname(wrapper);
        }

        List<Post> likedPosts = postMapper.selectBatchIds(likedPostIds);
        List<Long> likedCategoryIds = likedPosts.stream().map(Post::getCategoryId).distinct().collect(Collectors.toList());
        if (likedCategoryIds.isEmpty()) {
            QueryWrapper<Post> wrapper = publishedWrapper();
            wrapper.ne("forum_post.user_id", user.getId())
                    .orderByDesc("forum_post.create_time")
                    .last("LIMIT 10");
            return postMapper.selectListWithNickname(wrapper);
        }

        QueryWrapper<Post> wrapper = publishedWrapper();
        wrapper.in("forum_post.category_id", likedCategoryIds)
                .notIn("forum_post.id", likedPostIds)
                .ne("forum_post.user_id", user.getId())
                .orderByDesc("forum_post.create_time")
                .last("LIMIT 10");
        return postMapper.selectListWithNickname(wrapper);
    }

    private static QueryWrapper<Post> publishedWrapper() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_post.status", "PUBLISHED");
        wrapper.eq("forum_post.is_deleted", 0);
        return wrapper;
    }
}
