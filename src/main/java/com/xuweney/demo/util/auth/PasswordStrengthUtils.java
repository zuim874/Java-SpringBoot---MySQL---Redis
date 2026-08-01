package com.xuweney.demo.util.auth;

import com.xuweney.demo.config.security.PasswordStrengthConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 密码强度检验工具类
 */
@Component
public class PasswordStrengthUtils {
    private static PasswordStrengthConfig config;  // 静态引用

    @Autowired
    private PasswordStrengthConfig strengthConfig;

    @PostConstruct
    public void init() {
        config = this.strengthConfig;
    }

    /**
     * 密码强度等级枚举
     */
    public enum StrengthLevel {
        WEAK("弱", 1),
        MEDIUM("中", 2),
        STRONG("强", 3),
        VERY_STRONG("非常强", 4);

        private final String description;
        private final int level;

        StrengthLevel(String description, int level) {
            this.description = description;
            this.level = level;
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 密码强度检验结果
     */
    public static class StrengthResult {
        private final StrengthLevel level;
        private final String message;
        private final boolean valid;

        public StrengthResult(StrengthLevel level, String message, boolean valid) {
            this.level = level;
            this.message = message;
            this.valid = valid;
        }

        public StrengthLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValid() {
            return valid;
        }
    }

    /**
     * 检验密码强度（推荐使用）
     */
    public static StrengthResult checkStrength(String password) {
        // 使用静态配置(test.passwordstrength.status为false时跳过密码校验)
        if (config == null || !config.isStatus()) {
            if (password == null || password.isEmpty()) {
                return new StrengthResult(StrengthLevel.WEAK, "密码不能为空", false);
            }
            return new StrengthResult(StrengthLevel.MEDIUM, "密码强度检验未开启", true);
        }

        if (password == null || password.isEmpty()) {
            return new StrengthResult(StrengthLevel.WEAK, "密码不能为空", false);
        }

        int length = password.length();
        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find();

        // 计算得分
        int score = 0;

        // 长度评分
        if (length >= 12) {
            score += 3;
        } else if (length >= 10) {
            score += 2;
        } else if (length >= 8) {
            score += 1;
        }

        // 字符类型评分
        if (hasUpper) score += 1;
        if (hasLower) score += 1;
        if (hasDigit) score += 1;
        if (hasSpecial) score += 2;  // 特殊字符权重更高

        // 判定等级
        StrengthLevel level;
        String message;
        boolean valid;

        if (length < 8) {
            level = StrengthLevel.WEAK;
            message = "密码强度：弱（长度至少8位）";
            valid = false;
        } else if (score <= 4) {
            level = StrengthLevel.WEAK;
            message = "密码强度：弱（建议包含大小写字母、数字和特殊字符）";
            valid = false;
        } else if (score <= 6) {
            level = StrengthLevel.MEDIUM;
            message = "密码强度：中（建议增加特殊字符）";
            valid = true;
        } else if (score <= 8) {
            level = StrengthLevel.STRONG;
            message = "密码强度：强";
            valid = true;
        } else {
            level = StrengthLevel.VERY_STRONG;
            message = "密码强度：非常强";
            valid = true;
        }

        return new StrengthResult(level, message, valid);
    }

    /**
     * 快速校验（只返回是否通过）
     */
    public static boolean isValid(String password) {
        return checkStrength(password).isValid();
    }

    /**
     * 获取密码强度等级（只返回等级）
     */
    public static StrengthLevel getLevel(String password) {
        return checkStrength(password).getLevel();
    }
}
