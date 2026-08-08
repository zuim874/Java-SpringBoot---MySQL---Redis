package com.xuwenye.demo.util.oi;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 输入清洗工具
 * 1.sanitize：去除危险字符（XSS / SQL 注入防护），用于用户名/昵称/邮箱等展示文本
 * 2.dealEmail：统一邮箱格式（非 Gmail 转小写）
 * <p>
 * @author ZuiM
 */
@Component
public class SanitizeUtil {
    /**
     * 清洗输入：去除危险字符，防止 XSS 和 SQL 注入
     * 1.去掉首尾空格
     * 2.替换危险字符（< > ' -- 及 SQL 关键字）
     * <p>
     * @author ZuiM
     * @param input 原始输入
     * @return String 清洗后的输入
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
     * 统一邮箱格式：非 Gmail 邮箱转小写
     * 1.判断是否包含 @gmail.com
     * 2.非 Gmail 转小写，Gmail 保持原样（Gmail 大小写不敏感）
     * <p>
     * @author ZuiM
     * @param email 邮箱
     * @return String 统一后的邮箱
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
