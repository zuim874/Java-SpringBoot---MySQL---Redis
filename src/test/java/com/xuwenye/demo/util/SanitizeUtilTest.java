package com.xuwenye.demo.util;

import com.xuwenye.demo.util.oi.SanitizeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 输入清洗工具单元测试（纯单元测试，无需外部依赖）
 * 1.危险字符清洗：XSS 标签 / 单引号 / SQL 注释与关键字
 * 2.邮箱格式统一：非 Gmail 转小写，Gmail 保持原样
 * 3.边界输入：null / 空串 / 首尾空格
 * <p>
 * @author ZuiM
 */
class SanitizeUtilTest {

    private SanitizeUtil sanitizeUtil;

    @BeforeEach
    void setUp() {
        sanitizeUtil = new SanitizeUtil();
    }

    // ========== 1. 危险字符清洗 ==========

    /**
     * XSS 脚本标签被转义
     * <p>
     * @author ZuiM
     */
    @Test
    void xss脚本标签被转义() {
        String cleaned = sanitizeUtil.sanitize("<script>alert(1)</script>");
        assertFalse(cleaned.contains("<script>"));
        assertTrue(cleaned.contains("&lt;script&gt;"));
    }

    /**
     * SQL 注入关键字（select/drop/delete）被清除
     * <p>
     * @author ZuiM
     */
    @Test
    void sql注入关键字被清除() {
        String cleaned = sanitizeUtil.sanitize("select * from sys_user");
        assertFalse(cleaned.toLowerCase().contains("select"));
    }

    /**
     * 单引号与 SQL 注释符（--）被清除
     * <p>
     * @author ZuiM
     */
    @Test
    void 单引号与注释符被清除() {
        String cleaned = sanitizeUtil.sanitize("admin' -- ");
        assertFalse(cleaned.contains("'"));
        assertFalse(cleaned.contains("--"));
    }

    /**
     * 常见万能密码注入（or 1=1）被清除
     * <p>
     * @author ZuiM
     */
    @Test
    void 万能密码注入被清除() {
        String cleaned = sanitizeUtil.sanitize("1' or 1=1");
        assertFalse(cleaned.toLowerCase().contains("or 1=1"));
    }

    /**
     * 首尾空格被去除
     * <p>
     * @author ZuiM
     */
    @Test
    void 首尾空格被去除() {
        assertEquals("hello", sanitizeUtil.sanitize("  hello  "));
    }

    // ========== 2. 邮箱格式统一 ==========

    /**
     * 非 Gmail 邮箱统一转小写
     * <p>
     * @author ZuiM
     */
    @Test
    void 非Gmail邮箱转小写() {
        assertEquals("user@outlook.com", sanitizeUtil.dealEmail("User@Outlook.com"));
    }

    /**
     * Gmail 邮箱保持原样（Gmail 对大小写不敏感）
     * <p>
     * @author ZuiM
     */
    @Test
    void gmail邮箱保持原样() {
        assertEquals("User@gmail.com", sanitizeUtil.dealEmail("User@gmail.com"));
    }

    // ========== 3. 边界输入 ==========

    /**
     * null 输入返回 null（不做处理）
     * <p>
     * @author ZuiM
     */
    @Test
    void null输入返回null() {
        assertNull(sanitizeUtil.sanitize(null));
    }

    /**
     * 空串清洗后保持空串
     * <p>
     * @author ZuiM
     */
    @Test
    void 空串清洗保持空串() {
        assertEquals("", sanitizeUtil.sanitize(""));
    }
}
