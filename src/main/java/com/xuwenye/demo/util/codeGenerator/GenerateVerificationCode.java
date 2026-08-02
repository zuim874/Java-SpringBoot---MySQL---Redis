package com.xuwenye.demo.util.codeGenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GenerateVerificationCode {

    /** 使用 SecureRandom（加密安全随机数），避免 Random 可预测性 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成6位数字验证码
     */
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }
}
