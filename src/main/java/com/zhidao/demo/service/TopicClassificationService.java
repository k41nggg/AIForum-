package com.zhidao.demo.service;

import com.zhidao.demo.dto.RecommendCategoryResponse;
import com.zhidao.demo.entity.Post;

public interface TopicClassificationService {
    Long classifyPost(Post post);

    /**
     * 根据正在编辑的标题/正文推荐分类。
     * - 会读取数据库现有分类做匹配
     * - 若判断无合适分类，可按“仅父子关系”创建一个新分类
     * @return AI 推荐结果（包含理由/置信度/是否新建）
     */
    RecommendCategoryResponse recommendCategory(String title, String content);
}
