package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam String username,
                           @RequestParam String password) {
        // 输入清洗：去掉首尾空格和危险字符
        username = sanitize(username);
        password = sanitize(password);

        User user = userService.findUsernameforlogin(username);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(400, "账号或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(username);
        return Result.ok(Map.of(
                "token", token,
                "nickname", user.getNickname()
        ));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String password_exam,
                              @RequestParam String nickname) {
        // 输入清洗
        username = sanitize(username);
        password = sanitize(password);
        password_exam = sanitize(password_exam);
        nickname = sanitize(nickname);

        if (userService.findUsername(username) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (!password.equals(password_exam)) {
            return Result.error(400, "密码不一致");
        }
        if (username.length() < 2 || username.length() > 20) {
            return Result.error(400, "用户名长度需在2-20个字符之间");
        }
        if (password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setCreate_time(java.time.LocalDateTime.now());

        return userService.save(user)
                ? Result.ok("注册成功")
                : Result.error(500, "注册失败");
    }

    /**
     * 清洗输入：去除危险字符，防止 XSS 和 SQL 注入
     */
    private String sanitize(String input) {
        if (input == null) return null;
        // 去掉首尾空格
        input = input.trim();
        // 替换常见的危险字符
        input = input.replaceAll("<", "&lt;")
                     .replaceAll(">", "&gt;")
                     .replaceAll("'", "")
                     .replaceAll("--", "")
                     .replaceAll("(?i)select\\s", "")
                     .replaceAll("(?i)drop\\s", "")
                     .replaceAll("(?i)delete\\s", "")
                     .replaceAll("(?i)insert\\s", "")
                     .replaceAll("(?i)union\\s", "")
                     .replaceAll("(?i)or\\s+1=1", "");
        return input;
    }
}