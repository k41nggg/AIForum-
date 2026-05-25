package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_user_recommendation")
public class UserRecommendation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** JSON 数组，如 [1,2,3] */
    private String postIds;

    private String summary;

    private Integer actionCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
