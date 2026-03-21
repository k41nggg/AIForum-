package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zhidao.demo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE forum_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incLikeCount(@Param("postId") Long postId);

    @Select("SELECT forum_post.*, COALESCE(u.nickname, u.username) as user_nickname " +
            "FROM forum_post " +
            "LEFT JOIN sys_user u ON forum_post.user_id = u.id " +
            "${ew.customSqlSegment}")
    IPage<Post> selectPageWithNickname(IPage<Post> page, @Param(Constants.WRAPPER) Wrapper<Post> queryWrapper);

    @Select("SELECT forum_post.*, COALESCE(u.nickname, u.username) as user_nickname " +
            "FROM forum_post " +
            "LEFT JOIN sys_user u ON forum_post.user_id = u.id " +
            "WHERE forum_post.id = #{id} AND forum_post.is_deleted = 0")
    Post selectByIdWithNickname(@Param("id") Serializable id);
}
