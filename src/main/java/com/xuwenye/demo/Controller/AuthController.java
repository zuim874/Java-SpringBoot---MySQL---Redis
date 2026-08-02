package com.xuwenye.demo.Controller;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.dto.response.LoginResponse;
import com.xuwenye.demo.util.auth.JwtUtil;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils;
import com.xuwenye.demo.util.oi.SanitizeUtil;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import com.xuwenye.demo.util.codeGenerator.GenerateVerificationCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    @Value("${test.registerCodeCheck.status:false}")
    private boolean registerCodeCheckStatus;
    @Value("${test.registerEmailcheck.status:false}")
    private boolean registerEmailCheckStatus;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;
    private final GenerateVerificationCode generateVerificationCode;
    private final PasswordStrengthUtils passwordStrengthUtils;
    private final SanitizeUtil sanitizeUtil;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          EmailUtil emailUtil,
                          GenerateVerificationCode generateVerificationCode,
                          PasswordStrengthUtils passwordStrengthUtils,
                          SanitizeUtil sanitizeUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.emailUtil = emailUtil;
        this.generateVerificationCode = generateVerificationCode;
        this.passwordStrengthUtils = passwordStrengthUtils;
        this.sanitizeUtil = sanitizeUtil;
    }

    @RateLimit(window = 60, maxRequests = 3, message = "登录尝试过多，请稍后再试")
    @PostMapping("/login")
    public Result<?> login(@RequestParam String username,
                           @RequestParam String password) {
        // 输入清洗：去掉首尾空格（用户名防注入清洗；密码是敏感数据只 trim，不做字符替换以免篡改用户密码）
        username = sanitizeUtil.sanitize(username);
        password = password.trim();

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

    @RateLimit(window = 60, maxRequests = 3, message = "注册尝试过多，请稍后再试")
    @PostMapping("/register")
    public Result<?> register(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String password_exam,
                              @RequestParam String nickname,
                              @RequestParam String email,
                              @RequestParam String code) {
        // 输入清洗：用户名/昵称/邮箱防注入清洗；密码与验证码只 trim 不做字符替换
        username = sanitizeUtil.sanitize(username);
        password = password.trim();
        password_exam = password_exam.trim();
        nickname = sanitizeUtil.sanitize(nickname);
        email = sanitizeUtil.sanitize(email);
        code = code.trim();

        // 邮箱格式校验
        if (!EmailUtil.isValidEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }

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
                passwordStrengthUtils.checkStrength(password);

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

        try {
            boolean saved = userService.save(user);
            if (saved) {
                // 注册成功后清除 Redis 中的验证码，防止重复使用
                redisUtil.delete(redisKey);
                redisUtil.delete("verify_registerCode_limit:" + email);
                return Result.ok("注册成功");
            }
        } catch (DuplicateKeyException e) {
            return Result.error(400, "用户名已存在");
        }

        return Result.error(500, "注册失败");
    }

    @PostMapping("/send-registercode")
    public Result<?> sendVerificationCode(@RequestParam String email) {
        if (registerEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                // 1. 检查邮箱是否已注册（业务校验前置，避免无效请求占用限流窗口）
                if (userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱已被注册");
                }

                // 2. 检查是否频繁发送（防刷，先检查不写入）
                String sendLimitKey = "verify_registerCode_limit:" + email;
                // 原子操作：SET NX + EX（不存在才设置，设置60秒过期）
                Boolean success = redisUtil.setIfAbsent(
                        sendLimitKey, "1", 60, TimeUnit.SECONDS
                );
                // setIfAbsent 返回 false 表示 key 已存在（60秒内已发过）
                if (Boolean.FALSE.equals(success)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode.generateVerificationCode();

                // 4. 存入 Redis（设置过期时间 5分钟）
                String redisKey = "verify_registerCode:" + email;
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);

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
}