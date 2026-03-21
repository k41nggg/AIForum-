package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_comment")
public class Comment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private Long rootId;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer status;

    // 冗余字段用于展示，不持久化到数据库
    @TableField(exist = false)
    private String userNickname;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer isDeleted;
}
