package com.xuwenye.demo.util;

import com.xuwenye.demo.config.security.PasswordStrengthConfig;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils.StrengthLevel;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils.StrengthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码强度检验工具单元测试（纯单元测试，无需外部依赖）
 * 1.开启强度校验时的等级判定（弱/中/强/非常强）
 * 2.关闭校验时的放行逻辑
 * 3.空值/边界输入处理
 * <p>
 * @author ZuiM
 */
class PasswordStrengthUtilsTest {

    private PasswordStrengthUtils passwordStrengthUtils;

    /** 开启强度校验的配置 */
    private PasswordStrengthConfig enabledConfig() {
        PasswordStrengthConfig config = new PasswordStrengthConfig();
        config.setStatus(true);
        return config;
    }

    /** 关闭强度校验的配置 */
    private PasswordStrengthConfig disabledConfig() {
        PasswordStrengthConfig config = new PasswordStrengthConfig();
        config.setStatus(false);
        return config;
    }

    @BeforeEach
    void setUp() {
        passwordStrengthUtils = new PasswordStrengthUtils(enabledConfig());
    }

    // ========== 1. 开启校验时的等级判定 ==========

    /**
     * 弱密码：长度不足 8 位
     * <p>
     * @author ZuiM
     */
    @Test
    void 短密码判定为弱() {
        StrengthResult result = passwordStrengthUtils.checkStrength("Ab1!2");
        assertFalse(result.isValid());
        assertEquals(StrengthLevel.WEAK, result.getLevel());
    }

    /**
     * 弱密码：仅小写字母（长度够但字符类型单一）
     * <p>
     * @author ZuiM
     */
    @Test
    void 单一字符类型判定为弱() {
        StrengthResult result = passwordStrengthUtils.checkStrength("abcdefgh");
        assertFalse(result.isValid());
        assertEquals(StrengthLevel.WEAK, result.getLevel());
    }

    /**
     * 中等级：包含大小写与数字（8位以上）
     * <p>
     * @author ZuiM
     */
    @Test
    void 大小写加数字判定为中() {
        StrengthResult result = passwordStrengthUtils.checkStrength("Abcdef12");
        assertTrue(result.isValid());
        assertEquals(StrengthLevel.MEDIUM, result.getLevel());
    }

    /**
     * 强等级：包含大小写、数字、特殊字符
     * <p>
     * @author ZuiM
     */
    @Test
    void 多种字符类型判定为强() {
        StrengthResult result = passwordStrengthUtils.checkStrength("Abcdef12@");
        assertTrue(result.isValid());
        assertEquals(StrengthLevel.STRONG, result.getLevel());
    }

    /**
     * 非常强：12 位以上且包含四类字符
     * <p>
     * @author ZuiM
     */
    @Test
    void 超长且四类字符判定为非常强() {
        StrengthResult result = passwordStrengthUtils.checkStrength("Abcdef1234!@#");
        assertTrue(result.isValid());
        assertEquals(StrengthLevel.VERY_STRONG, result.getLevel());
    }

    // ========== 2. 关闭校验时的放行逻辑 ==========

    /**
     * 关闭校验后非空密码直接放行
     * <p>
     * @author ZuiM
     */
    @Test
    void 关闭校验非空密码放行() {
        PasswordStrengthUtils disabled = new PasswordStrengthUtils(disabledConfig());
        assertTrue(disabled.isValid("123456"));
        assertEquals(StrengthLevel.MEDIUM, disabled.getLevel("123456"));
    }

    /**
     * 关闭校验后空密码依然拦截
     * <p>
     * @author ZuiM
     */
    @Test
    void 关闭校验空密码拦截() {
        PasswordStrengthUtils disabled = new PasswordStrengthUtils(disabledConfig());
        assertFalse(disabled.isValid(""));
    }

    // ========== 3. 空值与边界输入 ==========

    /**
     * 空密码 / null 密码被拦截
     * <p>
     * @author ZuiM
     */
    @Test
    void 空密码拦截() {
        assertFalse(passwordStrengthUtils.isValid(null));
        assertFalse(passwordStrengthUtils.isValid(""));
    }

    /**
     * 恰好 8 位且含两类字符：中等级（合法）
     * <p>
     * @author ZuiM
     */
    @Test
    void 八位边界密码合法() {
        assertTrue(passwordStrengthUtils.isValid("Abcdef12"));
    }
}
