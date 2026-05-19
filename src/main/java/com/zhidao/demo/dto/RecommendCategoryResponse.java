package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCategoryResponse {
    /** AI 推荐的分类ID（可能为新创建的分类） */
    private Long categoryId;
    /** AI 推荐的分类名称 */
    private String categoryName;
    /** 父分类ID（仅用于提示层级） */
    private Long parentId;
    /** 父分类名称（仅用于提示层级） */
    private String parentName;
    /** 置信度 0~1 */
    private Double confidence;
    /** 解释/理由（中文） */
    private String reason;
    /** 是否新建了分类 */
    private Boolean created;
}
