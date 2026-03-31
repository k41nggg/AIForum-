package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.UserAction;
import com.zhidao.demo.mapper.UserActionMapper;
import com.zhidao.demo.service.UserActionService;
import org.springframework.stereotype.Service;

@Service
public class UserActionServiceImpl extends ServiceImpl<UserActionMapper, UserAction> implements UserActionService {
}
