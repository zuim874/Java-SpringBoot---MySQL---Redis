package com.xuwenye.demo.util.codeGenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 验证码生成工具
 * 1.使用 SecureRandom（加密安全随机数，不可预测）
 * 2.生成 6 位数字验证码
 * <p>
 * @author ZuiM
 */
@Component
public class GenerateVerificationCode {

    /** 使用 SecureRandom（加密安全随机数），避免 Random 可预测性 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成 6 位数字验证码
     * 1.循环 6 次取 0-9 随机数
     * 2.拼接为字符串返回
     * <p>
     * @author ZuiM
     * @return String 6 位数字验证码
     */
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }
}
