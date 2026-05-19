package com.zhidao.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhidao.demo.dto.ChatRequest;
import com.zhidao.demo.dto.Message;
import com.zhidao.demo.dto.RecommendCategoryResponse;
import com.zhidao.demo.entity.Category;
import com.zhidao.demo.entity.Post;
import com.zhidao.demo.service.AIService;
import com.zhidao.demo.service.CategoryService;
import com.zhidao.demo.service.TopicClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicClassificationServiceImpl implements TopicClassificationService {

    @Autowired
    private AIService aiService;

    @Autowired
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long classifyPost(Post post) {
        List<Category> categories = categoryService.list();
        String categoryNames = categories.stream().map(Category::getName).collect(Collectors.joining(", "));

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(null);
        chatRequest.setMessages(Arrays.asList(
                new Message("system", "你是论坛话题分类助手。请只返回最匹配的分类名称（不要输出其它内容）。"),
                new Message("user", "请根据帖子标题与正文，从下列分类中选择最合适的一项，只返回分类名称。\n\n分类列表: " + categoryNames + "\n\n标题: " + post.getTitle() + "\n\n正文: " + post.getContent())
        ));

        String categoryName = aiService.getCompletion(chatRequest)
                .map(response -> response.getChoices().get(0).getMessage().getContent())
                .block();

        if (categoryName == null) return null;
        String normalized = categoryName.trim();

        return categories.stream()
                .filter(category -> category.getName() != null && category.getName().equalsIgnoreCase(normalized))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public RecommendCategoryResponse recommendCategory(String title, String content) {
        title = title == null ? "" : title.trim();
        content = content == null ? "" : content.trim();

        List<Category> categories = categoryService.list(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        if (categories.isEmpty()) {
            // 没有分类时：创建一个默认父类 + 子类
            Category parent = ensureAiRootCategory();
            Category child = new Category();
            child.setName("其它");
            child.setParentId(parent.getId());
            child.setSort(0);
            categoryService.save(child);
            return new RecommendCategoryResponse(child.getId(), child.getName(), parent.getId(), parent.getName(), 0.0,
                    "系统尚无分类，已创建默认分类", true);
        }

        // 只把“叶子分类”优先提供给模型（也把父类给到，方便返回父类名）
        String categoriesJson = categories.stream()
                .map(c -> String.format(Locale.ROOT,
                        "{\"id\":%d,\"name\":%s,\"parentId\":%d}",
                        c.getId(), toJsonString(c.getName()), c.getParentId() == null ? 0 : c.getParentId()))
                .collect(Collectors.joining(",", "[", "]"));

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(null);
        chatRequest.setMessages(Arrays.asList(
                new Message("system",
                        "你是论坛话题智能分类助手。请严格只输出 JSON，不要输出Markdown代码块，不要输出多余文字。"),
                new Message("user",
                        "任务：根据用户正在编辑的帖子标题与正文，从数据库已有分类中选择最匹配的分类。\n" +
                                "如果没有任何分类合适，请提出一个新分类（只允许父子两级关系）：要么新建一个顶级分类，要么在某个顶级分类下新建子分类。\n\n" +
                                "现有分类 JSON 列表：" + categoriesJson + "\n\n" +
                                "请按如下 JSON 格式输出：\n" +
                                "{\n" +
                                "  \"matchType\": \"EXISTING\" 或 \"NEW\",\n" +
                                "  \"categoryName\": \"分类名称\",\n" +
                                "  \"parentName\": \"父分类名称（顶级分类则为空字符串）\",\n" +
                                "  \"confidence\": 0.0-1.0,\n" +
                                "  \"reason\": \"为什么推荐该分类（中文，尽量简短）\"\n" +
                                "}\n\n" +
                                "标题：" + title + "\n\n正文：\n" + content)
        ));

        String raw = aiService.getCompletion(chatRequest)
                .map(r -> r.getChoices().get(0).getMessage().getContent())
                .block();

        AiSuggest suggest = parseSuggest(raw);

        // 1) 优先匹配已有分类（忽略大小写、去空白）
        Category matched = findByName(categories, suggest.categoryName);
        if (matched != null) {
            Category parent = matched.getParentId() != null && matched.getParentId() != 0
                    ? categoryService.getById(matched.getParentId())
                    : null;
            return new RecommendCategoryResponse(
                    matched.getId(),
                    matched.getName(),
                    parent == null ? 0L : parent.getId(),
                    parent == null ? "" : parent.getName(),
                    suggest.confidence,
                    suggest.reason,
                    false
            );
        }

        // 2) 需要新建分类：仅父子关系
        // parentName 为空 -> 新建顶级分类；否则在 parentName 下建子类（若父类不存在，则建在 AI 根分类下）
        if (suggest.categoryName == null || suggest.categoryName.isBlank()) {
            return new RecommendCategoryResponse(null, null, null, null, 0.0, "AI 未返回分类名称", false);
        }

        Long parentId = 0L;
        String parentName = suggest.parentName == null ? "" : suggest.parentName.trim();
        Category parent;
        if (parentName.isBlank()) {
            parentId = 0L;
            parent = null;
        } else {
            parent = findByName(categories, parentName);
            if (parent == null) {
                // 找不到它声称的父类，就统一挂到 AI 根分类下
                parent = ensureAiRootCategory();
            }
            parentId = parent.getId();
        }

        // 创建子分类（或顶级分类）前，做一次幂等检查（忽略大小写）
        Category existed = categoryService.getOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getIsDeleted, 0)
                .apply("LOWER(name) = {0}", suggest.categoryName.trim().toLowerCase(Locale.ROOT))
                .last("LIMIT 1"));
        if (existed != null) {
            Category existedParent = existed.getParentId() != null && existed.getParentId() != 0
                    ? categoryService.getById(existed.getParentId())
                    : null;
            return new RecommendCategoryResponse(existed.getId(), existed.getName(),
                    existedParent == null ? 0L : existedParent.getId(),
                    existedParent == null ? "" : existedParent.getName(),
                    suggest.confidence, suggest.reason, false);
        }

        Category created = new Category();
        created.setName(suggest.categoryName.trim());
        created.setParentId(parentId);
        created.setSort(0);
        categoryService.save(created);

        return new RecommendCategoryResponse(
                created.getId(),
                created.getName(),
                parentId,
                parent == null ? "" : parent.getName(),
                suggest.confidence,
                suggest.reason,
                true
        );
    }

    private Category ensureAiRootCategory() {
        String rootName = "AI推荐";
        Category root = categoryService.getOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getIsDeleted, 0)
                .eq(Category::getParentId, 0)
                .eq(Category::getName, rootName)
                .last("LIMIT 1"));
        if (root != null) return root;

        Category created = new Category();
        created.setName(rootName);
        created.setParentId(0L);
        created.setSort(999);
        categoryService.save(created);
        return created;
    }

    private Category findByName(List<Category> categories, String name) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;
        for (Category c : categories) {
            if (c.getName() != null && c.getName().trim().equalsIgnoreCase(n)) {
                return c;
            }
        }
        return null;
    }

    private AiSuggest parseSuggest(String raw) {
        if (raw == null) {
            return new AiSuggest("", "", 0.0, "AI 无返回");
        }
        String text = stripMarkdownCodeFence(raw);
        try {
            JsonNode node = objectMapper.readTree(text);
            String categoryName = node.path("categoryName").asText("");
            String parentName = node.path("parentName").asText("");
            double confidence = node.path("confidence").asDouble(0.0);
            String reason = node.path("reason").asText("");
            return new AiSuggest(categoryName, parentName, confidence, reason);
        } catch (Exception e) {
            // 兜底：当模型只返回了分类名
            String fallbackName = text.trim();
            return new AiSuggest(fallbackName, "", 0.2, "AI 返回非 JSON，已按文本解析");
        }
    }

    private static String stripMarkdownCodeFence(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static String toJsonString(String s) {
        if (s == null) return "\"\"";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private static class AiSuggest {
        final String categoryName;
        final String parentName;
        final double confidence;
        final String reason;

        private AiSuggest(String categoryName, String parentName, double confidence, String reason) {
            this.categoryName = categoryName;
            this.parentName = parentName;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}
