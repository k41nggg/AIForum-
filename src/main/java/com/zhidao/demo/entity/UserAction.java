package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_user_action")
public class UserAction implements Serializable {

    public static final int TYPE_LIKE_POST = 1;
    public static final int TYPE_COLLECT_POST = 2;
    public static final int TYPE_LIKE_COMMENT = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    private Integer type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
