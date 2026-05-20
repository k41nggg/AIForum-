package com.zhidao.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.dto.UserSummaryVO;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.entity.UserFollow;

import java.util.List;

public interface UserFollowService extends IService<UserFollow> {

    void follow(Long followerId, Long followeeId);

    void unfollow(Long followerId, Long followeeId);

    boolean isFollowing(Long followerId, Long followeeId);

    List<UserSummaryVO> listFollowing(Long followerId);

    List<Post> feedPosts(Long followerId, int limit);
}
