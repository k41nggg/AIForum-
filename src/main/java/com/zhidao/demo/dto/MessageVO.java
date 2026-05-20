package com.zhidao.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private Integer type;
    private String title;
    private String content;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private String targetType;
    private Long targetId;
    private Long extraId;
    private Integer isRead;
    private LocalDateTime createTime;
}
