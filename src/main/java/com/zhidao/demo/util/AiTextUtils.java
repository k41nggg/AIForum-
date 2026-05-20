package com.zhidao.demo.util;

/**
 * AI 文本相关工具：用于控制 token 消耗（截断/清洗）。
 */
public class AiTextUtils {

    private AiTextUtils() {}

    /**
     * 对输入文本做温和清洗 + 截断。
     *
     * @param text 原始文本
     * @param maxChars 最大字符数（中文也按字符计）
     */
    /**
     * 送 AI 前去掉 Markdown/HTML 图片，仅保留文字上下文。
     */
    public static String stripImagesForAi(String text) {
        if (text == null) return "";
        return text
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]")
                .replaceAll("(?i)<img[^>]*>", "[图片]")
                .trim();
    }

    public static String prepareContext(String text, int maxChars) {
        if (text == null) return "";

        // 压缩过多空白，降低无意义 token
        String cleaned = stripImagesForAi(text)
                .replaceAll("\\r", "")
                .replaceAll("[\\t\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (maxChars <= 0) return cleaned;
        if (cleaned.length() <= maxChars) return cleaned;

        return cleaned.substring(0, maxChars) + "\n\n（内容过长已截断，以上为前" + maxChars + "字符）";
    }
}
