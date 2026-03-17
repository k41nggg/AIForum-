package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.Comment;
import com.zhidao.demo.mapper.CommentMapper;
import com.zhidao.demo.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
}
