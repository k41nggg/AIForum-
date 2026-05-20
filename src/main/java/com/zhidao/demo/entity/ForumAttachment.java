package com.zhidao.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("forum_attachment")
public class ForumAttachment implements Serializable {

    public static final int BIZ_TYPE_POST = 1;
    public static final int BIZ_TYPE_COMMENT = 2;

    public static final int STATUS_UPLOADED = 0;
    public static final int STATUS_BOUND = 1;
    public static final int STATUS_DELETED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
    private Long commentId;
    private Integer bizType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
