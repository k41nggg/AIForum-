package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zhidao.demo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT forum_comment.*, COALESCE(u.nickname, u.username) as user_nickname " +
            "FROM forum_comment " +
            "LEFT JOIN sys_user u ON forum_comment.user_id = u.id " +
            "${ew.customSqlSegment}")
    List<Comment> selectListWithNickname(@Param(Constants.WRAPPER) Wrapper<Comment> queryWrapper);
}
