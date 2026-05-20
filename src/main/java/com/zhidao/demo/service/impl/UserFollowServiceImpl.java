package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.dto.UserSummaryVO;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.entity.UserFollow;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.mapper.UserFollowMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.service.NotificationService;
import com.zhidao.demo.service.UserFollowService;
import com.zhidao.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements UserFollowService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        if (userService.getById(followeeId) == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        try {
            save(follow);
            notificationService.onUserFollowed(followeeId, followerId);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("已关注");
        }
    }

    @Override
    public void unfollow(Long followerId, Long followeeId) {
        remove(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return false;
        return count(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId)) > 0;
    }

    @Override
    public List<UserSummaryVO> listFollowing(Long followerId) {
        List<UserFollow> follows = list(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .orderByDesc(UserFollow::getCreateTime));
        if (follows.isEmpty()) return List.of();

        List<Long> followeeIds = follows.stream().map(UserFollow::getFolloweeId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(followeeIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return followeeIds.stream().map(id -> {
            User u = userMap.get(id);
            if (u == null) return null;
            UserSummaryVO vo = new UserSummaryVO();
            vo.setId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setBio(u.getBio());
            return vo;
        }).filter(v -> v != null).collect(Collectors.toList());
    }

    @Override
    public List<Post> feedPosts(Long followerId, int limit) {
        List<Long> followeeIds = list(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId))
                .stream().map(UserFollow::getFolloweeId).distinct().collect(Collectors.toList());
        if (followeeIds.isEmpty()) return List.of();

        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("forum_post.status", "PUBLISHED");
        wrapper.eq("forum_post.is_deleted", 0);
        wrapper.in("forum_post.user_id", followeeIds);
        wrapper.orderByDesc("forum_post.create_time");
        wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 50)));

        return enrichAuthorProfile(postMapper.selectListWithNickname(wrapper));
    }

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
}
