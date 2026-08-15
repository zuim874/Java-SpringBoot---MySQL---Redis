package com.xuwenye.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.service.AbstractServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口 MockMvc 集成测试（需 MySQL + Redis + ActiveMQ）
 * 1.登录：成功签发 Token / 密码错误 / 账号不存在
 * 2.注册：参数合法注册成功 / 用户名重复被拦截
 * 3.发送验证码：校验开关关闭时返回跳过提示
 * <p>
 * @author ZuiM
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends AbstractServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    /**
     * 构造测试用户（BCrypt 密码 = 明文 123456）
     * <p>
     * @author ZuiM
     * @return User 用户
     */
    private User buildUser() {
        User user = new User();
        user.setUsername(uniqueName("auth_user"));
        user.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa");
        user.setNickname("认证测试" + SUFFIX);
        user.setEmail(uniqueName("auth") + "@test.com");
        user.setStatus(1);
        user.setUserRole("ROLE_USER");
        user.setBalance(BigDecimal.ZERO);
        user.setCreateTime(LocalDateTime.now());
        userService.saveUser(user);
        return user;
    }

    /**
     * 清理测试用户（逻辑删除 + 清理缓存）
     * <p>
     * @author ZuiM
     * @param user 用户
     */
    private void cleanUser(User user) {
        if (user != null && user.getId() != null) {
            trackCacheKey("demo:user:active:" + user.getUsername());
            trackCacheKey("demo:user:all:" + user.getUsername());
            trackCacheKey("demo:user:id:" + user.getId());
            userMapper.deleteById(user.getId());
        }
    }

    // ========== 1. 登录 ==========

    /**
     * 正确账号密码登录成功并返回 Token
     * <p>
     * @author ZuiM
     */
    @Test
    void 登录成功返回token() throws Exception {
        User user = buildUser();

        mockMvc.perform(post("/api/auth/login")
                        .param("username", user.getUsername())
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.nickname").value(user.getNickname()));

        cleanUser(user);
    }

    /**
     * 密码错误登录失败（返回业务码 400）
     * <p>
     * @author ZuiM
     */
    @Test
    void 密码错误登录失败() throws Exception {
        User user = buildUser();

        mockMvc.perform(post("/api/auth/login")
                        .param("username", user.getUsername())
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.mes").value("账号或密码错误"));

        cleanUser(user);
    }

    /**
     * 账号不存在登录失败（返回业务码 400）
     * <p>
     * @author ZuiM
     */
    @Test
    void 账号不存在登录失败() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", uniqueName("no_user"))
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ========== 2. 注册 ==========

    /**
     * 合法参数注册成功
     * <p>
     * @author ZuiM
     */
    @Test
    void 合法参数注册成功() throws Exception {
        String username = uniqueName("register");
        String email = uniqueName("reg") + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                        .param("username", username)
                        .param("password", "Abc12345@")
                        .param("password_exam", "Abc12345@")
                        .param("nickname", "注册测试" + SUFFIX)
                        .param("email", email)
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // Result.ok(data) 中 mes 固定为"成功"，业务数据在 data 字段
                .andExpect(jsonPath("$.data").value("注册成功"));

        // 清理注册用户
        User registered = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (registered != null) {
            trackCacheKey("demo:user:active:" + username);
            userMapper.deleteById(registered.getId());
        }
    }

    /**
     * 用户名已存在时注册被拦截
     * <p>
     * @author ZuiM
     */
    @Test
    void 用户名重复注册被拦截() throws Exception {
        User user = buildUser();

        mockMvc.perform(post("/api/auth/register")
                        .param("username", user.getUsername())
                        .param("password", "Abc12345@")
                        .param("password_exam", "Abc12345@")
                        .param("nickname", "重复注册测试")
                        .param("email", uniqueName("dup") + "@test.com")
                        .param("code", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.mes").value("用户名已存在"));

        cleanUser(user);
    }

    // ========== 3. 发送验证码 ==========

    /**
     * 发送注册验证码（邮件校验开关关闭时返回跳过提示）
     * <p>
     * @author ZuiM
     */
    @Test
    void 发送注册验证码() throws Exception {
        mockMvc.perform(post("/api/auth/send-registercode")
                        .param("email", uniqueName("send") + "@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}