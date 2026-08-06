package com.xuwenye.demo.util.oi;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SanitizeUtil {
    /**
     * 清洗输入：去除危险字符，防止 XSS 和 SQL 注入
     */
    public String sanitize(String input) {
        if (input == null) return null;
        // 去掉首尾空格
        String input1 = input.trim();
        // 替换常见的危险字符
        String input2 = input1.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("'", "")
                .replaceAll("--", "")
                .replaceAll("(?i)select\\s", "")
                .replaceAll("(?i)drop\\s", "")
                .replaceAll("(?i)delete\\s", "")
                .replaceAll("(?i)insert\\s", "")
                .replaceAll("(?i)union\\s", "")
                .replaceAll("(?i)or\\s+1=1", "");
        return input2;
    }

    /**
     * @author ZuiM
     * @param email
     * @return String：非gmail邮箱转小写
     */
    public String dealEmail(String email) {
        if (!email.contains("@gmail.com")) {
            return email.toLowerCase(Locale.ROOT);
        }
        else {
            return email;
        }
    }
}
