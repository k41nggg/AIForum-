package com.zhidao.demo.util;

import com.zhidao.demo.dto.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * 项目中常用的 AI Tool 定义（OpenAI 兼容格式）
 */
public final class AiTools {

    private AiTools() {}

    /**
     * 内容审核 + 总结工具
     */
    public static ToolDefinition auditPostTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("audit_post")
                        .description("审核论坛帖子内容是否适合发布，并生成一句话总结。")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "approved", Map.of("type", "boolean", "description", "是否通过审核"),
                                        "reason", Map.of("type", "string", "description", "不通过原因（通过则为空）"),
                                        "summary", Map.of("type", "string", "description", "帖子内容的一句话中文总结（不超过60字）")
                                ),
                                "required", List.of("approved", "summary")
                        ))
                        .build())
                .build();
    }

    /**
     * 话题分类推荐工具
     */
    public static ToolDefinition classifyTopicTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("classify_topic")
                        .description("根据帖子标题和正文，从已有分类中选择最匹配的分类，或建议新建分类。")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "matchType", Map.of("type", "string", "enum", List.of("EXISTING", "NEW")),
                                        "categoryName", Map.of("type", "string", "description", "分类名称"),
                                        "parentName", Map.of("type", "string", "description", "父分类名称（顶级分类则为空）"),
                                        "confidence", Map.of("type", "number", "description", "置信度 0.0-1.0"),
                                        "reason", Map.of("type", "string", "description", "推荐理由")
                                ),
                                "required", List.of("matchType", "categoryName", "confidence", "reason")
                        ))
                        .build())
                .build();
    }

    /**
     * 个性化推荐工具
     */
    public static ToolDefinition recommendPostsTool() {
        return ToolDefinition.builder()
                .type("function")
                .function(ToolDefinition.FunctionDefinition.builder()
                        .name("recommend_posts")
                        .description("根据用户行为历史，从候选帖子中挑选最符合用户兴趣的帖子 ID 列表，并给出偏好总结。")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "postIds", Map.of("type", "array", "items", Map.of("type", "integer"),
                                                "description", "推荐的帖子 ID 列表（按相关性从高到低）"),
                                        "preferenceSummary", Map.of("type", "string", "description", "用户兴趣偏好的一句话总结")
                                ),
                                "required", List.of("postIds", "preferenceSummary")
                        ))
                        .build())
                .build();
    }
}