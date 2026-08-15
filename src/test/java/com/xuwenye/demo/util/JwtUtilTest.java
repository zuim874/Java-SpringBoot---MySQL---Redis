package com.xuwenye.demo.util;

import com.xuwenye.demo.util.auth.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具单元测试（纯单元测试，无需外部依赖）
 * 1.生成：Token 三段式结构，能解析出原用户名
 * 2.校验：合法 Token 通过、篡改/过期/非法 Token 拒绝
 * <p>
 * @author ZuiM
 */
class JwtUtilTest {

    /** HS256 密钥：要求 ≥ 32 字节 */
    private static final String SECRET = "test-jwt-secret-key-0123456789abcdef0123456789";

    private JwtUtil jwtUtil;
    private JwtUtil expiredJwtUtil;

    @BeforeEach
    void setUp() {
        // 有效期 1 小时
        jwtUtil = new JwtUtil(SECRET, 3600_000L);
        // 立即过期（exp 落在过去时间）
        expiredJwtUtil = new JwtUtil(SECRET, -1000L);
    }

    // ========== 1. 生成与解析 ==========

    /**
     * Token 为三段式 JWT 结构（头部.荷载.签名）
     * <p>
     * @author ZuiM
     */
    @Test
    void token为三段式结构() {
        String token = jwtUtil.generateToken("admin");
        assertEquals(3, token.split("\\.").length);
    }

    /**
     * 生成的 Token 能解析出原用户名
     * <p>
     * @author ZuiM
     */
    @Test
    void 生成token能解析出用户名() {
        String username = "test_user_" + System.nanoTime() % 100000;
        String token = jwtUtil.generateToken(username);
        assertEquals(username, jwtUtil.parseUsername(token));
    }

    /**
     * 不同用户生成的 Token 不同，且解析互不干扰
     * <p>
     * @author ZuiM
     */
    @Test
    void 不同用户token互不相同() {
        String tokenA = jwtUtil.generateToken("user_a");
        String tokenB = jwtUtil.generateToken("user_b");
        assertNotEquals(tokenA, tokenB);
        assertEquals("user_a", jwtUtil.parseUsername(tokenA));
        assertEquals("user_b", jwtUtil.parseUsername(tokenB));
    }

    // ========== 2. 校验 ==========

    /**
     * 合法且未过期的 Token 校验通过
     * <p>
     * @author ZuiM
     */
    @Test
    void 合法token校验通过() {
        String token = jwtUtil.generateToken("admin");
        assertTrue(jwtUtil.validate(token));
    }

    /**
     * 篡改签名（追加字符）后校验失败
     * <p>
     * @author ZuiM
     */
    @Test
    void 篡改token校验失败() {
        String token = jwtUtil.generateToken("admin");
        assertFalse(jwtUtil.validate(token + "x"));
    }

    /**
     * 已过期 Token 校验失败
     * <p>
     * @author ZuiM
     */
    @Test
    void 过期token校验失败() {
        String token = expiredJwtUtil.generateToken("admin");
        assertFalse(expiredJwtUtil.validate(token));
    }

    /**
     * 非法输入（空串 / null / 乱串）校验失败
     * <p>
     * @author ZuiM
     */
    @Test
    void 非法token校验失败() {
        assertFalse(jwtUtil.validate(""));
        assertFalse(jwtUtil.validate(null));
        assertFalse(jwtUtil.validate("not-a-jwt"));
        assertFalse(jwtUtil.validate("a.b.c"));
    }
}
