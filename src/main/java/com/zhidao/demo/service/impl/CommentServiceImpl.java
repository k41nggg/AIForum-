package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.Comment;
import com.zhidao.demo.entity.User;
import com.zhidao.demo.entity.UserAction;
import com.zhidao.demo.mapper.CommentMapper;
import com.zhidao.demo.mapper.UserMapper;
import com.zhidao.demo.mapper.UserActionMapper;
import com.zhidao.demo.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserActionMapper userActionMapper;

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        // 1. 查找指定帖子的所有评论
        List<Comment> allComments = this.list(new QueryWrapper<Comment>().eq("post_id", postId).orderByAsc("create_time"));

        // 2. 查找所有评论者的用户信息
        List<Long> userIds = allComments.stream().map(Comment::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return buildTree(allComments);
        }
        Map<Long, User> userMap = userMapper.selectList(new QueryWrapper<User>().in("id", userIds)).stream().collect(Collectors.toMap(User::getId, user -> user));

        // 3. 为每个评论设置用户昵称
        allComments.forEach(comment -> {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUserNickname(user.getNickname());
            }
        });

        // 4. 将列表转换为树形结构
        return buildTree(allComments);
    }

    private List<Comment> buildTree(List<Comment> comments) {
        // 筛选出所有根评论 (parentId 为 null 或 0)
        List<Comment> rootComments = comments.stream()
                .filter(comment -> comment.getParentId() == null || comment.getParentId() == 0)
                .collect(Collectors.toList());

        // 递归为每个根评论查找子评论
        rootComments.forEach(root -> root.setChildren(findChildren(root, comments)));

        return rootComments;
    }

    private List<Comment> findChildren(Comment parent, List<Comment> allComments) {
        return allComments.stream()
                // 筛选出当前父评论的所有直接子评论
                .filter(comment -> parent.getId().equals(comment.getParentId()))
                .peek(child -> {
                    // 递归为每个子评论查找其自身的子评论
                    child.setChildren(findChildren(child, allComments));
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public boolean likeComment(Long commentId, Long userId) {
        // 1. 检查用户是否已经点赞过
        long count = userActionMapper.selectCount(new QueryWrapper<UserAction>()
                .eq("user_id", userId)
                .eq("target_id", commentId)
                .eq("type", UserAction.TYPE_LIKE_COMMENT));
        if (count > 0) {
            return false; // 已经点赞过
        }

        // 2. 增加评论的点赞数
        this.update(new UpdateWrapper<Comment>().eq("id", commentId).setSql("like_count = like_count + 1"));

        // 3. 记录用户点赞行为
        UserAction action = new UserAction();
        action.setUserId(userId);
        action.setTargetId(commentId);
        action.setType(UserAction.TYPE_LIKE_COMMENT);
        userActionMapper.insert(action);

        return true;
    }

    @Transactional
    @Override
    public boolean unlikeComment(Long commentId, Long userId) {
        // 1. 删除用户的点赞记录
        int deletedRows = userActionMapper.delete(new QueryWrapper<UserAction>()
                .eq("user_id", userId)
                .eq("target_id", commentId)
                .eq("type", UserAction.TYPE_LIKE_COMMENT));

        if (deletedRows > 0) {
            // 2. 减少评论的点赞数
            this.update(new UpdateWrapper<Comment>().eq("id", commentId).setSql("like_count = like_count - 1"));
        }

        return deletedRows > 0;
    }
}
