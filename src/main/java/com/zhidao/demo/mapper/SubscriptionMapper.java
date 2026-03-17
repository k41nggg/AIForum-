package com.zhidao.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhidao.demo.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
