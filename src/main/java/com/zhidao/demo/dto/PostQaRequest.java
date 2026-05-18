package com.zhidao.demo.dto;

import lombok.Data;

/**
 * AI 问答请求体。
 */
@Data
public class PostQaRequest {
    private String question;
}
