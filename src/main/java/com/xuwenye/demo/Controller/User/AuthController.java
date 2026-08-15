package com.xuwenye.demo.Controller.User;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.MQProducer;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.dto.response.LoginResponse;
import com.xuwenye.demo.util.auth.JwtUtil;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils;
import com.xuwenye.demo.util.oi.SanitizeUtil;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import com.xuwenye.demo.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证接口：登录、注册、发送注册验证码
 * 1.登录：校验账号密码，签发 JWT Token
 * 2.注册：校验参数/验证码/密码强度后创建用户
 * 3.发送注册验证码：邮箱防刷限流后经消息队列发送
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@Slf4j
public class AuthController {
    private final RedisLockHelper redisLockHelper;
    /** 注册验证码校验开关 */
    @Value("${test.registerCodeCheck.status:false}")
    private boolean registerCodeCheckStatus;
    /** 注册邮件校验开关 */
    @Value("${test.registerEmailcheck.status:false}")
    private boolean registerEmailCheckStatus;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final PasswordStrengthUtils passwordStrengthUtils;
    private final SanitizeUtil sanitizeUtil;
    private final MQProducer mqProducer;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          PasswordStrengthUtils passwordStrengthUtils,
                          SanitizeUtil sanitizeUtil,
                          MQProducer mqProducer, RedisLockHelper redisLockHelper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.passwordStrengthUtils = passwordStrengthUtils;
        this.sanitizeUtil = sanitizeUtil;
        this.mqProducer = mqProducer;
        this.redisLockHelper = redisLockHelper;
    }

    /**
     * 用户登录
     * 1.输入清洗（用户名防注入、密码仅 trim）
     * 2.查询用户并校验密码（BCrypt 比对）
     * 3.校验账号状态（禁用则拦截）
     * 4.签发 JWT Token 并返回昵称
     * <p>
     * @author ZuiM
     * @param username 用户名
     * @param password 密码
     * @return Result 200 返回 token + nickname；400 账号或密码错误；403 账号禁用
     */
    @OperationLog("用户登录")
    @RateLimit(window = 60, maxRequests = 5, message = "登录尝试过多，请稍后再试")
    @PostMapping("/login")
    public Result<?> login(@RequestParam String username,
                           @RequestParam String password) {
        // 输入清洗：去掉首尾空格（用户名防注入清洗；密码是敏感数据只 trim，不做字符替换以免篡改用户密码）
        username = sanitizeUtil.sanitize(username);
        password = password.trim();
        User user = userService.findUserableUser(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(400, "账号或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }
        // 更新活跃时间
        userService.updateLastLoginTime(user.getId(), LocalDateTime.now());
        String token = jwtUtil.generateToken(username);
        return Result.ok(new LoginResponse(token, user.getNickname(), user.getUserRole()));
    }

    /**
     * 用户注册
     * 1.输入清洗与邮箱格式校验
     * 2.检查用户名是否已存在、两次密码是否一致、长度是否合法
     * 3.密码强度校验
     * 4.校验注册验证码（开关开启时）
     * 5.创建用户（BCrypt 加密密码）并清除验证码
     * <p>
     * @author ZuiM
     * @param username 用户名（2-20 字符）
     * @param password 密码
     * @param password_exam 确认密码
     * @param nickname 昵称
     * @param email 邮箱
     * @param code 注册验证码
     * @return Result 200 注册成功；400 参数/验证码错误；500 注册失败
     */
    @OperationLog("用户注册")
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
        if (userService.findAllUser(username) != null) {
            return Result.error(400, "用户名已存在");
        }
        if (!password.equals(password_exam)) {
            return Result.error(400, "密码不一致");
        }
        if (username.length() < 2 || username.length() > 20) {
            return Result.error(400, "用户名长度需在2-20个字符之间");
        }
        // 用户名格式校验：仅允许字母和数字，拒绝中文、下划线及其他特殊符号
        if (!username.matches("^[A-Za-z0-9]+$")) {
            return Result.error(400, "用户名只能包含字母和数字，不能包含中文、下划线及其他特殊符号");
        }
        // 密码强度检验（替换原来的简单长度校验）
        PasswordStrengthUtils.StrengthResult strengthResult =
                passwordStrengthUtils.checkStrength(password);

        if (!strengthResult.isValid()) {
            return Result.error(400, strengthResult.getMessage());
        }
        String redisKey = "verify_registerCode:" + email;
        String storedCode = (String) redisUtil.get(redisKey);
        if (registerCodeCheckStatus) {
            // 校验验证码是否正确且未过期
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
        String lockKey = "user:register:lock:" + email;
        boolean locked = false;
        try {
            // 分布式锁
            locked = redisLockHelper.tryLock(lockKey, 5, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error(429, "操作正在执行，请勿重复提交");
            }
            // 校验用户名
            User lockedUser = userService.findAllUser(username);
            if (lockedUser != null) {
                return Result.error(400, "用户已经存在");
            }
            if (userService.isEmailExist(email)) {
                return Result.error(400, "邮箱已经被注册");
            }
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setNickname(nickname);
            user.setStatus(1);
            // 保存小写（@gmail除外）
            email = sanitizeUtil.dealEmail(email);
            user.setEmail(email);
            user.setCreateTime(java.time.LocalDateTime.now());
            // 保存新用户信息
            boolean saved = userService.saveUser(user);
            if (saved) {
                // 注册成功后清除 Redis 中的验证码，防止重复使用
                redisUtil.delete(redisKey);
                redisUtil.delete("verify_registerCode_limit:" + email);
                return Result.ok("注册成功");
            }
            return Result.error(400, "注册失败");
        } catch (DuplicateKeyException e) {
            return Result.error(400, "用户名已存在");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();;
            return Result.error(429, "系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error(400, "注册失败，请重新尝试");
        } finally {
            // 释放锁
            if (locked) {
                redisLockHelper.unlock(lockKey);
            }
        }
    }

    /**
     * 发送注册验证码到邮箱
     * 0.邮箱格式校验
     * 1.检查邮箱是否已注册（业务校验前置，避免无效请求占用限流窗口）
     * 2.原子防刷（setIfAbsent，避免并发重复发送）
     * 3.经消息队列发送注册验证码邮件（EmailType=0）
     * <p>
     * @author ZuiM
     * @param email 邮箱
     * @return Result 200 发送成功/已跳过；400 校验/限流失败
     */
    @PostMapping("/send-registercode")
    public Result<?> sendVerificationCode(@RequestParam String email) {
        if (registerEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                // 处理邮箱格式
                final String realEmail = sanitizeUtil.dealEmail(email);
                // 1. 检查邮箱是否已注册（业务校验前置，避免无效请求占用限流窗口）
                if (userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱已被注册");
                }
                // 2. 检查是否频繁发送（防刷，先检查不写入）(修复：统一转换为小写，防止)
                final String sendLimitKey = "verify_registerCode_limit:" + realEmail;
                // 原子操作：SET NX + EX（不存在才设置，设置60秒过期）
                Boolean success = redisUtil.setIfAbsent(sendLimitKey, "1");
                // setIfAbsent 返回 false 表示 key 已存在（60秒内已发过）
                if (Boolean.FALSE.equals(success)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }
                // 消息队列发送
                mqProducer.sendEmailTask(email, 0);
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