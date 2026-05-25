package com.zhidao.demo.dto;

import com.zhidao.demo.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationResponse {
    private List<Post> posts;
    private boolean cached;
    private LocalDateTime updatedAt;
    private String summary;
    private Integer actionCount;
    private String hint;
    /** 刷新冷却剩余秒数（>0 表示不可刷新） */
    private Long cooldownSeconds;
}
