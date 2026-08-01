package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.dto.response.LoginResponse;
import com.xuweney.demo.util.auth.JwtUtil;
import com.xuweney.demo.util.auth.PasswordStrengthUtils;
import com.xuweney.demo.util.oi.SanitizeUtil;
import com.xuweney.demo.util.email.EmailUtil;
import com.xuweney.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    @Value("${test.registerCodeCheck.status:true}")
    private boolean registerCodeCheckStatus;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          EmailUtil emailUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.emailUtil = emailUtil;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam String username,
                           @RequestParam String password) {
        // 输入清洗：去掉首尾空格和危险字符
        username = SanitizeUtil.sanitize(username);
        password = SanitizeUtil.sanitize(password);

        User user = userService.findUsernameforlogin(username);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(400, "账号或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(username);
        return Result.ok(new LoginResponse(token, user.getNickname()));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String password_exam,
                              @RequestParam String nickname,
                              @RequestParam String email,
                              @RequestParam String code) {
        // 输入清洗
        username = SanitizeUtil.sanitize(username);
        password = SanitizeUtil.sanitize(password);
        password_exam = SanitizeUtil.sanitize(password_exam);
        nickname = SanitizeUtil.sanitize(nickname);
        email = SanitizeUtil.sanitize(email);
        code = SanitizeUtil.sanitize(code);

        if (userService.findUsername(username) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (!password.equals(password_exam)) {
            return Result.error(400, "密码不一致");
        }
        if (username.length() < 2 || username.length() > 20) {
            return Result.error(400, "用户名长度需在2-20个字符之间");
        }
        // 4. 密码强度检验（替换原来的简单长度校验）
        PasswordStrengthUtils.StrengthResult strengthResult =
                PasswordStrengthUtils.checkStrength(password);

        if (!strengthResult.isValid()) {
            return Result.error(400, strengthResult.getMessage());
        }

        String redisKey = "verify_registerCode:" + email;
        String storedCode = (String) redisUtil.get(redisKey);
        if (registerCodeCheckStatus) {
            // 1. 校验验证码是否正确且未过期
            if (storedCode == null) {
                return Result.error(400, "验证码已过期，请重新获取");
            }
            if (!storedCode.equals(code)) {
                return Result.error(400, "验证码错误");
            }
        }
        else {
            System.out.println("已跳过验证");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setEmail(email);
        user.setCreateTime(java.time.LocalDateTime.now());

        boolean saved = userService.save(user);
        if (saved) {
            // 注册成功后清除 Redis 中的验证码，防止重复使用
            redisUtil.delete(redisKey);
            redisUtil.delete("verify_code_limit:" + email);
            return Result.ok("注册成功");
        }
        return Result.error(500, "注册失败");
    }

    @PostMapping("/send-registercode")
    public Result<?> sendVerificationCode(@RequestParam String email) {
        if (registerCodeCheckStatus) {
            try {
                // 1. 检查邮箱是否已注册
                if (userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱已被注册");
                }

                // 2. 检查是否频繁发送（防刷）
                String redisKey = "verify_registerCode:" + email;
                String sendLimitKey = "verify_registerCode_limit:" + email;

                // 检查是否在60秒内重复发送
                if (redisUtil.hasKey(sendLimitKey)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400,"请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode();

                // 4. 存入 Redis（设置过期时间 5分钟）
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);
                // 发送限制标记（60秒过期）
                redisUtil.set(sendLimitKey, "1", 60, TimeUnit.SECONDS);

                // 5. 发送邮件（异步发送）
                emailUtil.sendRegisterVerificationCode(email, code);

                return Result.ok("注册验证码已发送到您的邮箱，请注意查收");

            } catch (Exception e) {
                e.printStackTrace();    // 控制台定位报错代码行数
                return Result.error(400, "发送验证码失败：" + e.getMessage());
            }
        }
        else {
            return Result.ok("已跳过注册邮箱验证");
        }
    }

    /**
     * 生成6位数字验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}