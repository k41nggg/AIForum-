package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhidao.demo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
