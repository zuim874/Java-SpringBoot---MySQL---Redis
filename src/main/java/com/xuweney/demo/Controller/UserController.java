package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.annotation.RateLimit;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.auth.JwtUtil;
import com.xuweney.demo.util.codeGenerator.GenerateVerificationCode;
import com.xuweney.demo.util.email.EmailUtil;
import com.xuweney.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 用户自助操作接口（用户操作自己的账号）
 */
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;
    private final GenerateVerificationCode generateVerificationCode;
    public UserController(UserService userService,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          EmailUtil emailUtil,
                          GenerateVerificationCode generateVerificationCode) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.emailUtil = emailUtil;
        this.generateVerificationCode = generateVerificationCode;
    }

    @Value("${test.recoverCodeCheck.status:false}")
    private boolean recoverCodeCheckStatus;
    @Value("${test.deleteCodeCheck.status:false}")
    private boolean deleteCodeCheckStatus;

    /**
     * 用户注销自己的账号（逻辑删除）
     */
    @RateLimit(window = 60, maxRequests = 3, message = "注销尝试过多，请稍后再试")
    @DeleteMapping("/delete_user")
    public Result<?> deleteSelf(
            @RequestHeader("Authorization") String token,
            @RequestParam String email,
            @RequestParam String code
    ) {
        // 1.校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 2.从 token 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User targetUser = userService.findUsernameforlogin(loginUsername);
        if (targetUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 3.校验验证码
        String redisKey = "verify_deleteCode:" + email;
        String storedCode = (String) redisUtil.get(redisKey);
        if (deleteCodeCheckStatus) {
            if (storedCode == null) {
                return Result.error(400, "验证码已过期，请重新获取");
            }
            if (!storedCode.equals(code)) {
                return Result.error(400, "验证码错误");
            }
        } else {
            System.out.println("已跳过验证");
        }

        // 4.删除自己（天然不存在越权问题）
        boolean deleteSuccess = userService.deleteById(targetUser.getId());
        if (deleteSuccess) {
            return Result.ok("账号注销成功");
        } else {
            return Result.error(500, "账号注销失败，请稍后重试");
        }
    }

    /**
     * 用户恢复自己的账号（通过邮箱验证码验证身份）
     * <p>
     * 注意：恢复操作不能让用户登录后再进行，因为已逻辑删除的用户无法登录。
     * 因此采用「用户名 + 邮箱 + 验证码」的方式验证身份。
     */
    @RateLimit(window = 60, maxRequests = 3, message = "账号恢复尝试过多，请稍后再试")
    @PutMapping("/recover_user")
    public Result<?> recoverUserByUserParam(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String code
    ) {
        // 1: 检查用户名（查找已删除的用户）
        User user = userService.findUsernameforRecover(username);
        if (user == null) {
            return Result.error(400, "请检查用户名是否正确");
        }
        // 2: 检查邮箱是否匹配
        if (!user.getEmail().equals(email)) {
            return Result.error(400, "请检查绑定的邮箱是否正确");
        }

        String redisKey = "verify_recoverCode:" + email;
        String storedCode = (String) redisUtil.get(redisKey);
        // 3: 校验验证码
        if (recoverCodeCheckStatus) {
            if (storedCode == null) {
                return Result.error(400, "验证码已过期，请重新获取");
            }
            if (!storedCode.equals(code)) {
                return Result.error(400, "验证码错误");
            }
        } else {
            System.out.println("已跳过验证");
        }

        // 4: 执行恢复
        boolean recoverSuccess = userService.recoverById(user.getId());
        if (recoverSuccess) {
            // 恢复成功后清除验证码，防止重复使用
            redisUtil.delete(redisKey);
            return Result.ok("恢复用户成功");
        } else {
            return Result.error(400, "恢复失败，该用户不存在");
        }
    }

    /**
     * 发送账号恢复验证码到邮箱
     */
    @RateLimit(window = 60, maxRequests = 3, message = "注销邮箱发送过多，请稍后再试")
    @PostMapping("/send-deletecode")
    public Result<?> sendDeleteCode(@RequestParam String email) {
        if (deleteCodeCheckStatus) {
            try {
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }

                // 2. 检查是否频繁发送（防刷）
                String redisKey = "verify_deleteCode:" + email;
                String sendLimitKey = "verify_deleteCode_limit:" + email;

                if (redisUtil.hasKey(sendLimitKey)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode.generateVerificationCode();

                // 4. 存入 Redis（5分钟过期）
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);
                // 发送限制标记（60秒过期）
                redisUtil.set(sendLimitKey, "1", 60, TimeUnit.SECONDS);

                // 5. 发送邮件（异步发送）
                emailUtil.sendDeleteAccountCode(email, code);

                return Result.ok("账号注销验证码已发送到您的邮箱，请注意查收");

            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(400, "发送验证码失败：" + e.getMessage());
            }
        } else {
            return Result.ok("已跳过账号注销邮箱验证");
        }
    }

    /**
     * 发送账号恢复验证码到邮箱
     */
    @RateLimit(window = 60, maxRequests = 3, message = "恢复邮箱发送过多，请稍后再试")
    @PostMapping("/send-recovercode")
    public Result<?> sendRecoverCode(@RequestParam String email) {
        if (recoverCodeCheckStatus) {
            try {
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }

                // 2. 检查是否频繁发送（防刷）
                String redisKey = "verify_recoverCode:" + email;
                String sendLimitKey = "verify_recoverCode_limit:" + email;

                if (redisUtil.hasKey(sendLimitKey)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode.generateVerificationCode();

                // 4. 存入 Redis（5分钟过期）
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);
                // 发送限制标记（60秒过期）
                redisUtil.set(sendLimitKey, "1", 60, TimeUnit.SECONDS);

                // 5. 发送邮件（异步发送）
                emailUtil.sendRecoverAccountCode(email, code);

                return Result.ok("账号恢复验证码已发送到您的邮箱，请注意查收");

            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(400, "发送验证码失败：" + e.getMessage());
            }
        } else {
            return Result.ok("已跳过账号恢复邮箱验证");
        }
    }
}