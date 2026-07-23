package com.xuweney.demo.util;

public class SanitizeUtil {
    /**
     * 清洗输入：去除危险字符，防止 XSS 和 SQL 注入
     */
    public static String Sanitize(String input) {
        if (input == null) return null;
        // 去掉首尾空格
        input = input.trim();
        // 替换常见的危险字符
        input = input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("'", "")
                .replaceAll("--", "")
                .replaceAll("(?i)select\\s", "")
                .replaceAll("(?i)drop\\s", "")
                .replaceAll("(?i)delete\\s", "")
                .replaceAll("(?i)insert\\s", "")
                .replaceAll("(?i)union\\s", "")
                .replaceAll("(?i)or\\s+1=1", "");
        return input;
    }
}
