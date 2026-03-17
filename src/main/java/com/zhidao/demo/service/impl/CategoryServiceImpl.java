package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhidao.demo.entity.Category;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.mapper.CategoryMapper;
import com.zhidao.demo.mapper.PostMapper;
import com.zhidao.demo.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private PostMapper postMapper;

    @Override
    public List<Category> listTree() {
        // 获取所有未删除的分类
        List<Category> all = list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));

        // 组建树形结构（简单的两层示例，如需无限级可用递归）
        return all.stream()
                .filter(c -> c.getParentId() == 0)
                .map(parent -> {
                    // 这里由于Category实体类中没有children字段，通常我们会用一个DTO来返回树形结构
                    // 如果实体类不能改，可以只返回列表并在前端处理，或者这里仅做一级筛选
                    return parent;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getHotCategories() {
        // 统计逻辑：按分类统计帖子数量（仅统计已发布状态2的帖子）
        // 这里为了简单，直接查所有分类并注入帖子数
        List<Category> categories = list();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category category : categories) {
            Long count = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                    .eq(Post::getCategoryId, category.getId())
                    .eq(Post::getStatus, 2));

            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("name", category.getName());
            map.put("postCount", count);
            result.add(map);
        }

        // 按帖子数量降序排列
        result.sort((a, b) -> ((Long) b.get("postCount")).compareTo((Long) a.get("postCount")));

        // 返回前10个
        return result.stream().limit(10).collect(Collectors.toList());
    }
}
