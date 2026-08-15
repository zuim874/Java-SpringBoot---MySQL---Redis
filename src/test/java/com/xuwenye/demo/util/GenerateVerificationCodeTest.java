package com.xuwenye.demo.util;

import com.xuwenye.demo.util.codeGenerator.GenerateVerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码生成工具单元测试（纯单元测试，无需外部依赖）
 * 1.格式：固定 6 位数字
 * 2.随机性：批量生成无重复
 * 3.边界：连续生成多次均符合格式
 * <p>
 * @author ZuiM
 */
class GenerateVerificationCodeTest {

    private GenerateVerificationCode codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new GenerateVerificationCode();
    }

    /**
     * 验证码格式：6 位纯数字
     * <p>
     * @author ZuiM
     */
    @Test
    void 验证码为六位数字() {
        String code = codeGenerator.generateVerificationCode();
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    /**
     * 批量生成 1000 个验证码无重复（随机性验证）
     * <p>
     * @author ZuiM
     */
    @Test
    void 批量生成无重复() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(codeGenerator.generateVerificationCode());
        }
        assertEquals(1000, codes.size());
    }

    /**
     * 连续生成 100 次均符合 6 位数字格式
     * <p>
     * @author ZuiM
     */
    @Test
    void 连续生成均符合格式() {
        for (int i = 0; i < 100; i++) {
            assertTrue(codeGenerator.generateVerificationCode().matches("\\d{6}"));
        }
    }
}
