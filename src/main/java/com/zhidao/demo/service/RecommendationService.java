package com.zhidao.demo.service;

import com.zhidao.demo.dto.RecommendationResponse;
import com.zhidao.demo.entity.User;

public interface RecommendationService {

    /** 读取已保存的推荐结果，不调 AI */
    RecommendationResponse getRecommendations(User user);

    /** 调用 AI 分析并更新缓存，5 分钟内不可重复刷新 */
    RecommendationResponse refreshRecommendations(User user);
}
