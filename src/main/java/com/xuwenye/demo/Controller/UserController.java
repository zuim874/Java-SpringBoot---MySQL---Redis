package com.xuwenye.demo.Controller;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.FileStorageService;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import com.xuwenye.demo.util.codeGenerator.GenerateVerificationCode;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;
    public UserController(UserService userService,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          EmailUtil emailUtil,
                          GenerateVerificationCode generateVerificationCode,
                          FileStorageService fileStorageService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.emailUtil = emailUtil;
        this.generateVerificationCode = generateVerificationCode;
        this.fileStorageService = fileStorageService;
    }

    @Value("${test.recoverCodeCheck.status:false}")
    private boolean recoverCodeCheckStatus;
    @Value("${test.recoverEmailCheck.status:false}")
    private boolean recoverEmailCheckStatus;
    @Value("${test.deleteCodeCheck.status:false}")
    private boolean deleteCodeCheckStatus;
    @Value("${test.deleteEmailCheck.status:false}")
    private boolean deleteEmailCheckStatus;

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
     * 发送账号删除验证码到邮箱
     */
    @RateLimit(window = 60, maxRequests = 3, message = "注销邮箱发送过多，请稍后再试")
    @PostMapping("/send-deletecode")
    public Result<?> sendDeleteCode(@RequestParam String email) {
        if (deleteEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }

                // 2. 原子防刷：首次设置限流 key 成功才放行（60 秒内重复请求被拦截）
                String sendLimitKey = "verify_deleteCode_limit:" + email;
                Boolean firstRequest = redisUtil.setIfAbsent(sendLimitKey, "1", 60, TimeUnit.SECONDS);
                if (Boolean.FALSE.equals(firstRequest)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode.generateVerificationCode();

                // 4. 存入 Redis（5分钟过期）
                String redisKey = "verify_deleteCode:" + email;
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);

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
        if (recoverEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }

                // 2. 原子操作
                String sendLimitKey = "verify_recoverCode_limit:" + email;
                Boolean success = redisUtil.setIfAbsent(sendLimitKey, "1", 60, TimeUnit.SECONDS);
                if (Boolean.FALSE.equals(success)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }

                // 3. 生成6位随机验证码
                String code = generateVerificationCode.generateVerificationCode();

                // 4. 存入 Redis（5分钟过期）
                String redisKey = "verify_recoverCode:" + email;
                redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);

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

    /**
     * 获取当前登录用户信息（含头像、邮箱等）
     */
    @GetMapping("/me")
    public Result<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        // 1. 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 2. 查询当前用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User user = userService.findUsernameforlogin(loginUsername);
        if (user == null) {
            return Result.error(400, "用户不存在");
        }

        // 3. 清空敏感字段再返回（密码、逻辑删除标记不外泄）
        user.setPassword(null);
        user.setIsDeleted(null);
        return Result.ok(user);
    }

    /**
     * 上传/更新头像（multipart/form-data，字段名 file）
     */
    @RateLimit(window = 60, maxRequests = 5, message = "头像上传过于频繁，请稍后再试")
    @PostMapping("/avatar")
    public Result<?> uploadAvatar(@RequestHeader("Authorization") String token,
                                  @RequestParam("file") MultipartFile file) {
        // 1. 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 2. 查询当前用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User currentUser = userService.findUsernameforlogin(loginUsername);
        if (currentUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 3. 保存文件（类型/大小校验在 FileStorageService 内完成）
        String avatarUrl;
        try {
            avatarUrl = fileStorageService.storeAvatar(file);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(500, e.getMessage());
        }

        // 4. 更新数据库并清理缓存
        boolean updated = userService.updateAvatar(currentUser.getId(), avatarUrl);
        if (!updated) {
            return Result.error(500, "头像保存失败，请稍后重试");
        }

        return Result.ok(avatarUrl);
    }
}