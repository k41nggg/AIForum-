package com.zhidao.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 本地存储根目录（相对或绝对路径） */
    private String dir = "uploads";

    /** 单文件最大字节数，默认 5MB */
    private long maxSize = 5 * 1024 * 1024;

    /** 允许的 MIME 类型，逗号分隔 */
    private String allowedTypes = "image/jpeg,image/png,image/gif,image/webp";

    public Set<String> allowedTypeSet() {
        Set<String> set = new HashSet<>();
        if (allowedTypes == null || allowedTypes.isBlank()) {
            return set;
        }
        Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(set::add);
        return set;
    }
}
