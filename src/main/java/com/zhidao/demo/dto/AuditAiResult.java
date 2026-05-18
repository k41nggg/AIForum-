package com.zhidao.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 审核 + 总结的结构化结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditAiResult {
    /** 是否适合发布 */
    private boolean approved;
    /** 不通过原因（approved=false 时可能有值） */
    private String reason;
    /** AI 总结（尽量一句话，中文） */
    private String summary;
}
