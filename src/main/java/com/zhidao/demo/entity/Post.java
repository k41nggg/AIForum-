package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_post")
public class Post implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String status; // PUBLISHED, AUDIT_PENDING, DELETED
    private String auditReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
