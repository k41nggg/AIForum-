package com.zhidao.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhidao.demo.entity.Category;

import java.util.List;
import java.util.Map;

public interface CategoryService extends IService<Category> {
    List<Category> listTree();
    List<Map<String, Object>> getHotCategories();
}
