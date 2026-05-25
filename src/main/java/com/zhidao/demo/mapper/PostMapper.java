package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zhidao.demo.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;
import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE forum_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incLikeCount(@Param("postId") Long postId);

    @Update("UPDATE forum_post SET collect_count = collect_count + 1 WHERE id = #{postId}")
    int incCollectCount(@Param("postId") Long postId);

    @Update("UPDATE forum_post SET collect_count = GREATEST(collect_count - 1, 0) WHERE id = #{postId}")
    int decCollectCount(@Param("postId") Long postId);

    @Select("SELECT forum_post.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM forum_post " +
            "LEFT JOIN sys_user u ON forum_post.user_id = u.id " +
            "${ew.customSqlSegment}")
    @Results(id = "postWithUserMap", value = {
            @Result(property = "userNickname", column = "user_nickname"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    IPage<Post> selectPageWithNickname(IPage<Post> page, @Param(Constants.WRAPPER) Wrapper<Post> queryWrapper);

    @Select("SELECT forum_post.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM forum_post " +
            "LEFT JOIN sys_user u ON forum_post.user_id = u.id " +
            "WHERE forum_post.id = #{id} AND forum_post.is_deleted = 0")
    @ResultMap("postWithUserMap")
    Post selectByIdWithNickname(@Param("id") Serializable id);

    @Select("SELECT forum_post.*, u.nickname AS user_nickname, u.avatar AS user_avatar " +
            "FROM forum_post " +
            "LEFT JOIN sys_user u ON forum_post.user_id = u.id " +
            "${ew.customSqlSegment}")
    @ResultMap("postWithUserMap")
    List<Post> selectListWithNickname(@Param(Constants.WRAPPER) Wrapper<Post> queryWrapper);
}
