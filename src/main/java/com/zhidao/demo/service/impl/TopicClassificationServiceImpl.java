package com.zhidao.demo.service.impl;

import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.entity.Category;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.TopicClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicClassificationServiceImpl implements TopicClassificationService {

    @Autowired
    private AIService aiService;

    @Autowired
    private CategoryService categoryService;

    @Override
    public Long classifyPost(Post post) {
        List<Category> categories = categoryService.list();
        String categoryNames = categories.stream().map(Category::getName).collect(Collectors.joining(", "));

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel("deepseek-coder");
        chatRequest.setMessages(java.util.Arrays.asList(
                new Message("system", "You are a helpful assistant that classifies forum posts into categories."),
                new Message("user", "Based on the following post title and content, which of the following categories does it belong to? Please only return the category name.\n\nCategories: " + categoryNames + "\n\nTitle: " + post.getTitle() + "\n\nContent: " + post.getContent())
        ));

        String categoryName = aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent())
                .block();

        return categories.stream()
                .filter(category -> category.getName().equalsIgnoreCase(categoryName))
                .map(Category::getId)
                .findFirst()
                .orElse(null); // Return null if no matching category is found
    }
}
