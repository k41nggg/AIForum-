package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.Subscription;
import com.zhidao.demo.mapper.SubscriptionMapper;
import com.zhidao.demo.service.SubscriptionService;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionServiceImpl extends ServiceImpl<SubscriptionMapper, Subscription> implements SubscriptionService {
}
