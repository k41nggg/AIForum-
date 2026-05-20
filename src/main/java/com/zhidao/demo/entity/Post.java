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

    /**
     * 仅用于展示：通过 PostMapper 的关联查询 selectPageWithNickname/selectByIdWithNickname 注入。
     * 对应 SQL 列名 user_nickname。
     */
    @TableField(exist = false)
    private String userNickname;

    /** 展示用：发帖人头像，对应 user_avatar */
    @TableField(exist = false)
    private String userAvatar;

    private Long categoryId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String status; // PUBLISHED, AUDIT_PENDING, DELETED
    private String auditReason;

    private String aiSummary;
    private LocalDateTime aiSummaryUpdateTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
