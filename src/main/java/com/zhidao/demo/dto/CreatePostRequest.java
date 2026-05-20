package com.zhidao.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private Long categoryId;
    private String title;
    private String content;
    /** 发帖时绑定的附件 ID（上传接口返回） */
    private List<Long> attachmentIds;
}
