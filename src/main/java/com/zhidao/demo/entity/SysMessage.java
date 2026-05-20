package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private Long senderId;
    private Integer type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private Long extraId;
    private Integer isRead;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String senderNickname;

    @TableField(exist = false)
    private String senderAvatar;
}
