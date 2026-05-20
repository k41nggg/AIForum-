package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_user_follow")
public class UserFollow implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关注者 */
    private Long followerId;
    /** 被关注的用户 */
    private Long followeeId;
    private LocalDateTime createTime;
}
