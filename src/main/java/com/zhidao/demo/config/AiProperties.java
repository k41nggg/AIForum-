package com.zhidao.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 硅基流动 OpenAI 兼容根地址，需包含 /v1 */
    private String baseUrl = "https://api.siliconflow.cn/v1";

    private String apiKey;

    /** 平台模型名，如 Qwen/Qwen2.5-7B-Instruct */
    private String model = "Qwen/Qwen3-8B";
}
