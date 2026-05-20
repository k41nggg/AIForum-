package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zhidao.demo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT forum_comment.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM forum_comment " +
            "LEFT JOIN sys_user u ON forum_comment.user_id = u.id " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(property = "userNickname", column = "user_nickname"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    List<Comment> selectListWithNickname(@Param(Constants.WRAPPER) Wrapper<Comment> queryWrapper);
}
