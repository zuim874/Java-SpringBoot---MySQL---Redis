package com.xuwenye.demo.Controller;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.FileStorageService;
import com.xuwenye.demo.Service.MQProducer;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.PasswordStrengthUtils;
import com.xuwenye.demo.util.email.EmailType;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.oi.SanitizeUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import com.xuwenye.demo.util.redis.RedisLockHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * 用户自助操作接口（用户操作自己的账号）
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/user")
@Validated
@Slf4j  // 打印错误堆栈日志
public class UserController {
    private final UserService userService;
    private final RedisUtil redisUtil;
    private final FileStorageService fileStorageService;
    private final SanitizeUtil sanitizeUtil;
    private final MQProducer mqProducer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthUtils passwordStrengthUtils;
    private final RedisLockHelper redisLockHelper;
    public UserController(UserService userService,
                          RedisUtil redisUtil,
                          FileStorageService fileStorageService,
                          SanitizeUtil sanitizeUtil,
                          MQProducer mqProducer,
                          PasswordEncoder passwordEncoder,
                          PasswordStrengthUtils passwordStrengthUtils,
                          RedisLockHelper redisLockHelper) {
        this.userService = userService;
        this.redisUtil = redisUtil;
        this.fileStorageService = fileStorageService;
        this.sanitizeUtil = sanitizeUtil;
        this.mqProducer = mqProducer;
        this.passwordEncoder = passwordEncoder;
        this.passwordStrengthUtils = passwordStrengthUtils;
        this.redisLockHelper = redisLockHelper;
    }

    /** 账号恢复验证码校验开关 */
    @Value("${test.recoverCodeCheck.status:false}")
    private boolean recoverCodeCheckStatus;
    /** 账号恢复邮件校验开关 */
    @Value("${test.recoverEmailCheck.status:false}")
    private boolean recoverEmailCheckStatus;
    /** 账号注销验证码校验开关 */
    @Value("${test.deleteCodeCheck.status:false}")
    private boolean deleteCodeCheckStatus;
    /** 账号注销邮件校验开关 */
    @Value("${test.deleteEmailCheck.status:false}")
    private boolean deleteEmailCheckStatus;
    /** 换绑邮箱验证码校验开关（高危操作，默认开启） */
    @Value("${test.changeEmailCodeCheck.status:true}")
    private boolean changeEmailCodeCheckStatus;

    /**
     * 用户注销自己的账号（逻辑删除）
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.校验验证码
     * 3.删除自己（天然不存在越权问题）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param email 当前绑定邮箱
     * @param code 邮箱验证码
     * @return Result<?> 200/400/401/500：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 3, message = "账号注销尝试过多，请稍后再试")
    @UserCheck
    @DeleteMapping("/delete_user")
    public Result<?> deleteSelf(User currentUser,
                                @RequestParam String email,
                                @RequestParam String code) {
        try {
            // 1.校验验证码（key 用统一小写后的邮箱，与 EmailUtil 存储验证码时的 dealEmail 处理保持一致）
            String redisKey = "verify_deleteCode:" + sanitizeUtil.dealEmail(email);
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
            // 2.删除自己（currentUser 由切面注入，天然不存在越权问题）
            boolean deleteSuccess = userService.deleteUserById(currentUser.getId());
            if (deleteSuccess) {
                return Result.ok("账号注销成功");
            } else {
                return Result.error(500, "账号注销失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("用户注销失败", e);
            return Result.error(500, "账号注销失败，请稍后重试");
        }
    }

    /**
     * 用户恢复自己的账号（通过邮箱验证码验证身份）
     * 1.检查用户名（查找已删除的用户）
     * 2.检查邮箱是否匹配
     * 3.校验验证码
     * 4.执行恢复
     * <p>
     * 注意：恢复操作不能让用户登录后再进行，因为已逻辑删除的用户无法登录。
     * 因此采用「用户名 + 邮箱 + 验证码」的方式验证身份。
     * <p>
     * @author ZuiM
     * @param username 用户名（已删除用户）
     * @param email 绑定邮箱
     * @param code 邮箱验证码
     * @return Result<?> 200/400：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 3, message = "账号恢复尝试过多，请稍后再试")
    @PutMapping("/recover_user")
    public Result<?> recoverUserByUserParam(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String code
    ) {
        try {
            // 1: 检查用户名（查找已删除的用户）
            User user = userService.findDeletedUserByUsername(username);
            if (user == null) {
                return Result.error(400, "请检查用户名是否正确");
            }
            // 2: 检查邮箱是否匹配（统一小写比较，与注册/发送验证码时的 dealEmail 处理保持一致）
            if (!user.getEmail().equals(sanitizeUtil.dealEmail(email))) {
                return Result.error(400, "请检查绑定的邮箱是否正确");
            }
            String redisKey = "verify_recoverCode:" + sanitizeUtil.dealEmail(email);
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
            boolean recoverSuccess = userService.recoverUserById(user.getId());
            if (recoverSuccess) {
                // 恢复成功后清除验证码，防止重复使用
                redisUtil.delete(redisKey);
                return Result.ok("恢复用户成功");
            } else {
                return Result.error(400, "恢复失败，该用户不存在");
            }
        } catch (Exception e) {
            log.error("用户恢复失败");
            return Result.error(500, "用户恢复失败，请稍后再试");
        }
    }

    /**
     * 发送账号删除验证码到邮箱（ip+邮箱双重限流：1次每分钟）
     * 1.接收 Authorization 请求头（Spring Security 认证，未登录返回 401 JSON；此处双保险校验）
     * 2.邮箱格式校验
     * 3.统一邮箱格式（转为小写，与验证码存储 key 的 dealEmail 处理保持一致）
     * 4.检查邮箱是否已注册
     * 5.原子防刷（防止并发数据不一致）
     * 6.发送邮件（消息队列）
     * <p>
     * 注意：本接口需登录后才能调用（Spring Security 保护，未登录返回 401）。
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Authorization: Bearer xxx，请求头）
     * @param email 已注册的邮箱
     * @return Result<?> 200/400/401：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 3, message = "注销验证码发送频繁，请稍后再试")
    @PostMapping("/send-deletecode")
    public Result<?> sendDeleteCode(@RequestHeader(value = "Authorization", required = false) String token,
                                    @RequestParam String email) {
        // 0. 双保险校验登录态（正常情况下 Spring Security 已认证通过，此处防止未来配置变化导致未认证进入）
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        if (deleteEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                String realEmail = sanitizeUtil.dealEmail(email);
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(realEmail)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }
                // 2. 原子防刷：首次设置限流 key 成功才放行（60 秒内同一账号重复请求被拦截）
                String sendLimitKey = "verify_deleteCode_limit:" + realEmail;
                Boolean firstRequest = redisUtil.setIfAbsent(sendLimitKey, "1");
                if (Boolean.FALSE.equals(firstRequest)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }
                // 3. 发送邮件（消息队列，type=2 账号注销验证）
                mqProducer.sendEmailTask(realEmail, 2);
                return Result.ok("账号注销验证码已发送到您的邮箱，请注意查收");
            } catch (Exception e) {
                log.error("账号注销验证码发送失败", e);
                return Result.error(400, "发送验证码失败：" + e.getMessage());
            }
        } else {
            return Result.ok("已跳过账号注销邮箱验证");
        }
    }

    /**
     * 发送账号恢复验证码到邮箱（ip+邮箱双重限流：1次每分钟）
     * 1.邮箱格式校验
     * 2.检查邮箱是否已注册
     * 3.原子防刷（防止并发数据不一致）
     * 4.发送邮件（消息队列）
     * <p>
     * @author ZuiM
     * @param email 已注册的邮箱
     * @return Result<?> 200/400：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 1, message = "恢复验证码发送频繁，请稍后再试")
    @PostMapping("/send-recovercode")
    public Result<?> sendRecoverCode(@RequestParam String username,
                                     @RequestParam String email) {
        if (recoverEmailCheckStatus) {
            try {
                // 1: 检查用户名（查找已删除的用户）
                User user = userService.findDeletedUserByUsername(username);
                if (user == null) {
                    return Result.error(400, "请检查用户名是否正确");
                }
                String realEmail = sanitizeUtil.dealEmail(email);
                // 2: 检查邮箱是否匹配
                if (!user.getEmail().toLowerCase().equals(realEmail)) {
                    return Result.error(400, "请检查绑定的邮箱是否正确");
                }
                // 3. 原子操作
                String sendLimitKey = "verify_recoverCode_limit:" + realEmail;
                Boolean success = redisUtil.setIfAbsent(sendLimitKey, "1");
                if (Boolean.FALSE.equals(success)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }
                // 4. 发送邮件（异步发送，消息队列）
                mqProducer.sendEmailTask(realEmail, 1);
                return Result.ok("账号恢复验证码已发送到您的邮箱，请注意查收");
            } catch (Exception e) {
                log.error("账号恢复验证码发送失败", e);
                return Result.error(400, "发送账号恢复验证码失败：" + e.getMessage());
            }
        } else {
            return Result.ok("已跳过账号恢复邮箱验证");
        }
    }

    /**
     * 获取当前登录用户信息（含头像、邮箱等）
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.清空敏感字段再返回（密码、逻辑删除标记不外泄）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @return Result<?> 200/400/401：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 3, message = "用户资料刷新频繁，请稍后再试")
    @UserCheck
    @GetMapping("/me")
    public Result<?> getCurrentUser(User currentUser) {
        try {
            // currentUser 由切面注入（已校验 Token 并查询用户）
            // 清空敏感字段再返回（密码、逻辑删除标记不外泄）
            currentUser.setPassword(null);
            currentUser.setIsDeleted(null);
            return Result.ok(currentUser);
        } catch (Exception e) {
            log.error("用户资料获取失败", e);
            return Result.error(500, "用户资料获取失败");
        }
    }

    /**
     * 上传/更新头像（multipart/form-data，字段名 file）
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.分布式锁（看门狗）防并发 + 锁内双重检查
     * 3.保存文件（类型/大小校验在 FileStorageService 内完成）
     * 4.更新数据库并清理缓存
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param file 头像图片文件（jpg/png/gif/webp，大小不超过 2MB）
     * @return Result<?> 200/400/401/500：成功返回新头像 URL
     */
    @RateLimit(window = 60, maxRequests = 3, message = "头像上传频繁，请稍后再试")
    @UserCheck
    @PostMapping("/avatar")
    public Result<?> uploadAvatar(User currentUser,
                                  @RequestParam("file") MultipartFile file) {
        // 1.分布式锁（看门狗模式）：同一用户同一时刻只允许一个更新头像请求
        String lockKey = "user:change_avatar:lock:" + currentUser.getId();
        boolean locked = false;
        try {
            // 尝试获取锁
            locked = redisLockHelper.tryLock(lockKey, 3, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error(429, "操作正在处理，请勿重复提交");
            }
            // 2.锁内双重检查（防止等待锁期间用户状态变化）
            User lockUser = userService.findUserableUser(currentUser.getUsername());
            if (lockUser == null) {
                return Result.error(400, "用户不存在");
            }
            // 3. 保存文件（类型/大小校验在 FileStorageService 内完成）
            String avatarUrl;
            try {
                avatarUrl = fileStorageService.storeAvatar(file);
            } catch (IllegalArgumentException e) {
                log.error("头像上传失败", e);
                return Result.error(400, e.getMessage());
            } catch (RuntimeException e) {
                log.error("头像上传失败", e);
                return Result.error(500, e.getMessage());
            }
            // 4. 更新数据库并清理缓存
            boolean updated = userService.updateAvatar(lockUser.getId(), avatarUrl);
            if (!updated) {
                return Result.error(500, "头像保存失败，请稍后重试");
            }
            return Result.ok(avatarUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            log.error("头像上传失败", e);
            return Result.error(500, "系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error(500, "头像保存失败，请稍后重试");
        } finally {
            // 5.释放分布式锁
            if (locked) {
                redisLockHelper.unlock(lockKey);
            }
        }
    }

    /**
     * 更新用户资料（仅昵称，邮箱换绑请调用独立的 /change_email 接口）
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.校验昵称（非空、长度 1~20）
     * 3.分布式锁（看门狗）防并发 + 锁内双重检查
     * 4.更新昵称（不涉及邮箱字段）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param nickname 新昵称
     * @return Result<?> 200/400/401/500：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 3, message = "资料更新频繁，请稍后再试")
    @UserCheck
    @PutMapping("/update_profile")
    public Result<?> updateProfile(User currentUser,
                                   @RequestParam String nickname) {
            // 1. 校验昵称（非空、长度 1~20）
            if (nickname == null || nickname.trim().isEmpty()) {
                return Result.error(400, "昵称不能为空");
            }
            String realNickname = nickname.trim();
            if (realNickname.length() > 20) {
                return Result.error(400, "昵称长度不能超过 20 个字符");
            }
            String lockKey = "user:update_profile:lock:" + currentUser.getId();
            boolean locked = false;
            // 2. 分布式锁（看门狗模式）：同一用户同一时刻只允许一个资料更新请求
            try {
                // 尝试上锁
                locked = redisLockHelper.tryLock(lockKey, 5, TimeUnit.SECONDS);
                if (!locked) {
                    return Result.error(429, "操作正在处理，请勿重复提交");
                }
                // 3. 锁内双重检查（防止等待锁期间用户状态变化）
                User lockUser = userService.findUserableUser(currentUser.getUsername());
                if (lockUser == null) {
                    return Result.error(400, "用户不存在");
                }
                // 4. 更新昵称（不涉及邮箱字段）
                boolean updated = userService.updateProfile(lockUser.getId(), realNickname, null);
                if (!updated) {
                    return Result.error(500, "资料更新失败，请稍后重试");
                }
                return Result.ok("资料更新成功");
            } catch (InterruptedException e) {
                // 获取锁被中断：恢复中断状态，避免吞掉中断信号
                Thread.currentThread().interrupt();
                return Result.error(500, "系统繁忙，请稍后再试");
            } catch (Exception e) {
                log.error("用户资料更新失败", e);   // 控制台定位报错代码行数
                return Result.error(500, "资料更新失败，请稍后重试");
            } finally {
                // 5. 释放分布式锁（unlock 会停止看门狗续期；helper 内部自动判断当前线程是否持有，不会误释放他人锁）
                if (locked) {
                    redisLockHelper.unlock(lockKey);
                }
            }
    }

    /**
     * 换绑邮箱（独立接口，不归属资料更新）
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.统一邮箱格式（转为小写）
     * 3.新邮箱不能与当前绑定邮箱相同
     * 4.旧邮箱必须与当前绑定邮箱一致
     * 5.邮箱格式校验
     * 6.邮箱不能已被绑定
     * 7.校验新旧邮箱验证码
     * 8.分布式锁（看门狗）防并发 + 锁内双重检查
     * 9.更新邮箱
     * 10.清除redis缓存验证码
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param oldEmail 当前绑定邮箱
     * @param oldEmail_code 当前邮箱验证码
     * @param newEmail 新邮箱
     * @param newEmail_code 新邮箱验证码
     * 需先调用 /send-changeEmail 获取两组验证码：
     * type=3 旧邮箱验证码（发到旧邮箱）、type=4 新邮箱验证码（发到新邮箱）
     */
    @RateLimit(window = 60, maxRequests = 3, message = "邮箱更换过于频繁，请稍后再试")
    @UserCheck
    @PutMapping("/change_email")
    public Result<?> changeEmail(User currentUser,
                                 @RequestParam String oldEmail,
                                 @RequestParam String oldEmail_code,
                                 @RequestParam String newEmail,
                                 @RequestParam String newEmail_code) {
        // 1. 统一邮箱格式（与 send-changeEmail 发送验证码时的处理保持一致，避免大小写导致 key 对不上）
        String realOldEmail = sanitizeUtil.dealEmail(oldEmail.trim());
        String realNewEmail = sanitizeUtil.dealEmail(newEmail.trim());
        String realNowEmail = sanitizeUtil.dealEmail(currentUser.getEmail());
        // 2. 新邮箱不能与当前相同
        if (realNewEmail.equals(realNowEmail)) {
            return Result.error(400, "新邮箱与当前邮箱相同，无需换绑");
        }
        // 3. 旧邮箱必须与当前绑定邮箱一致（防止使用他人邮箱完成验证）
        if (!realOldEmail.equals(realNowEmail)) {
            return Result.error(400, "旧邮箱与当前绑定邮箱不一致");
        }
        // 4. 新邮箱格式校验
        if (!EmailUtil.isValidEmail(realNewEmail)) {
            return Result.error(400, "新邮箱格式不正确");
        }
        // 5. 新邮箱不能已被其他账号使用
        if (userService.isEmailExist(realNewEmail)) {
            return Result.error(400, "该新邮箱已被注册");
        }
        // 6. 校验旧邮箱验证码（verify_oldEmailCheckCode:{旧邮箱}）
        if (changeEmailCodeCheckStatus) {
            String oldRedisKey = "verify_oldEmailCheckCode:" + realOldEmail;
            String oldStoredCode = (String) redisUtil.get(oldRedisKey);
            if (oldStoredCode == null) {
                return Result.error(400, "旧邮箱验证码已过期，请重新获取");
            }
            if (!oldStoredCode.equals(oldEmail_code)) {
                return Result.error(400, "旧邮箱验证码错误");
            }
            // 7. 校验新邮箱验证码（verify_newEmailCheckCode:{新邮箱}）
            String newRedisKey = "verify_newEmailCheckCode:" + realNewEmail;
            String newStoredCode = (String) redisUtil.get(newRedisKey);
            if (newStoredCode == null) {
                return Result.error(400, "新邮箱验证码已过期，请重新获取");
            }
            if (!newStoredCode.equals(newEmail_code)) {
                return Result.error(400, "新邮箱验证码错误");
            }
        }
        String lockKey = "user:change_email:lock:" + currentUser.getId();
        boolean locked = false;
        try {
            // 获取锁
            locked = redisLockHelper.tryLock(lockKey, 5, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error(429, "操作正在处理，请勿反复提交");
            }
            // 8. 锁内双重检查：重新查用户，防止等待锁期间用户状态变化
            User lockUser = userService.findUserableUser(currentUser.getUsername());
            if (lockUser == null) {
                return Result.error(400, "用户不存在");
            }
            // 9. 锁内复核：旧邮箱必须仍与当前绑定邮箱一致（防止锁等待期间邮箱被其他请求改掉）
            if (!realOldEmail.equals(sanitizeUtil.dealEmail(lockUser.getEmail()))) {
                return Result.error(400, "旧邮箱与当前绑定邮箱不一致");
            }
            // 10. 更新邮箱（不涉及昵称字段）
            boolean updated = userService.updateProfile(lockUser.getId(), null, realNewEmail);
            if (!updated) {
                return Result.error(500, "换绑邮箱失败，请稍后重试");
            }
            // 11. 换绑成功后清除验证码，防止重复使用（用统一后的邮箱保证 key 一致）
            redisUtil.delete("verify_oldEmailCheckCode:" + realOldEmail);
            redisUtil.delete("verify_newEmailCheckCode:" + realNewEmail);
            return Result.ok("邮箱更换成功");
        } catch (InterruptedException e) {
            // 获取锁被中断：恢复中断状态，避免吞掉中断信号
            Thread.currentThread().interrupt();
            return Result.error(500, "系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("邮箱换绑失败", e);   // 控制台定位报错代码行数
            return Result.error(500, "邮箱换绑失败，请稍后重试");
        } finally {
            // 12. 释放分布式锁（unlock 会停止看门狗续期；helper 内部自动判断当前线程是否持有，不会误释放他人锁）
            if (locked) {
                redisLockHelper.unlock(lockKey);
            }
        }
    }

    /**
     * 发送换绑邮箱验证码
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.校验邮件类型（仅支持旧邮箱/新邮箱验证）
     * 3.邮箱格式校验（统一转换为小写）
     * 4.业务校验（旧邮箱是否为当前当前绑定邮箱，新邮箱是否已经绑定）
     * 5.原子防刷（防止并发数据不一致）
     * 6.发送邮件
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param email 收件邮箱
     * @param type 邮件类型
     * type=3（EmailType.ChangeOld）：发送到旧邮箱，email 必须与当前绑定邮箱一致
     * type=4（EmailType.ChangeNew）：发送到新邮箱，email 必须未被其他账号使用
     */
    @RateLimit(window = 60, maxRequests = 5, message = "更改邮箱验证码发送过于频繁，请稍后重试！")
    @UserCheck
    @PostMapping("/send-changeEmail")
    public Result<?> sendChangeEmail(User currentUser,
                                     @RequestParam String email,
                                     @RequestParam Integer type) {
        // 1. 校验邮件类型（仅支持旧邮箱验证 / 新邮箱验证两种）
        if (type == null
                || (!type.equals(EmailType.ChangeOld.getCode()) && !type.equals(EmailType.ChangeNew.getCode()))) {
            return Result.error(400, "邮件类型不正确");
        }

        // 2. 邮箱格式校验 + 统一小写
        if (!EmailUtil.isValidEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }
        String realEmail = sanitizeUtil.dealEmail(email);

        // 3. 业务校验（前置，避免无效请求占用限流窗口）
        if (type.equals(EmailType.ChangeOld.getCode())) {
            // 旧邮箱验证：目标邮箱必须是当前绑定邮箱，防止用他人邮箱接收验证码
            if (!realEmail.equals(sanitizeUtil.dealEmail(currentUser.getEmail()))) {
                return Result.error(400, "旧邮箱与当前绑定邮箱不一致");
            }
        } else {
            // 新邮箱验证：目标邮箱必须未被其他账号使用
            if (userService.isEmailExist(realEmail)) {
                return Result.error(400, "该新邮箱已被注册");
            }
        }

        // 4. 原子防刷：首次设置限流 key 成功才放行（同一邮箱同一类型 5 分钟内不重复发送）
        String sendLimitKey = "verify_changeEmailCode_limit:" + type + ":" + realEmail;
        Boolean firstRequest = redisUtil.setIfAbsent(sendLimitKey, "1");
        if (Boolean.FALSE.equals(firstRequest)) {
            long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
            return Result.error(400, "请等待 " + ttl + " 秒后再试");
        }

        // 5. 发送邮件（消息队列，EmailType 3=旧邮箱验证、4=新邮箱验证）
        mqProducer.sendEmailTask(realEmail, type);
        return Result.ok("换绑邮箱验证码已发送到您的邮箱，请注意查收");
    }

    /**
     * 用户修改密码
     * 1.@UserCheck 切面校验登录态并注入当前用户
     * 2.清洗、校验密码（锁外首次校验）
     * 3.密码强度校验
     * 4.分布式锁（看门狗）防并发重复提交
     * 5.锁内双重检查（重新查库 + 重新校验旧密码）
     * 6.修改密码
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入）
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param newPassword_check 确认新密码
     * @return 400/401/500/200 身份验证失败/用户不存在/修改失败/修改成功
     */
    @RateLimit(window = 60, maxRequests = 5, message = "更改密码过于频繁，请稍后重试！")
    @UserCheck
    @PostMapping("/change_password")
    public Result<?> changePassword(User currentUser,
                                    @RequestParam String oldPassword,
                                    @RequestParam String newPassword,
                                    @RequestParam String newPassword_check) {
        // 1.校验旧密码（锁外第一次检查，用切面注入的 currentUser）
        oldPassword = oldPassword.trim();
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return Result.error(400, "原密码错误");
        }
        newPassword = newPassword.trim();
        newPassword_check = newPassword_check.trim();
        if (oldPassword.equals(newPassword)) {
            return Result.error(400, "新密码不能与旧密码相同");
        }
        if (!newPassword.equals(newPassword_check)) {
            return Result.error(400, "两次密码不一致");
        }
        // 2.密码强度检验
        PasswordStrengthUtils.StrengthResult strengthResult =
                passwordStrengthUtils.checkStrength(newPassword);
        if (!strengthResult.isValid()) {
            return Result.error(400, strengthResult.getMessage());
        }
        // 3.分布式锁（看门狗模式）：同一用户同一时刻只允许一个改密码请求（防并发重复提交）
        String lockKey = "user:change_password:lock:" + currentUser.getId();
        boolean locked = false;
        try {
            // 尝试获取锁：最多等待 5 秒；拿到后由 Redisson 看门狗自动续期，
            // 业务执行多久锁就持有多久，不会因锁租约到期被提前释放（unlock 后停止续期）
            locked = redisLockHelper.tryLock(lockKey, 5, TimeUnit.SECONDS);
            if (!locked) {
                return Result.error(429, "操作正在处理，请勿重复提交");
            }
            // 4. 锁内双重检查（防止等待锁期间数据被其他请求修改）
            // 4.1 重新查询用户，确保拿到最新数据
            User lockedUser = userService.findUserableUser(currentUser.getUsername());
            if (lockedUser == null) {
                return Result.error(400, "用户不存在");
            }
            // 4.2 重新校验旧密码，防止锁等待期间密码已被其他请求修改
            if (!passwordEncoder.matches(oldPassword, lockedUser.getPassword())) {
                return Result.error(400, "原密码错误");
            }
            // 5. 更改密码（updateById 更新已有用户，避免误用 save 导致插入新记录）
            String encodedPassword = passwordEncoder.encode(newPassword);
            boolean updated = userService.updatePassword(lockedUser.getId(), encodedPassword);
            if (updated) {
                return Result.ok("密码修改成功");
            }
            return Result.error(500, "密码修改失败");
        } catch (InterruptedException e) {
            // 获取锁被中断：恢复中断状态，避免吞掉中断信号
            Thread.currentThread().interrupt();
            return Result.error(500, "系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("用户密码修改失败", e);   // 控制台定位报错代码行数
            return Result.error(500, "密码修改失败，请稍后重试");
        } finally {
            // 6. 释放分布式锁（unlock 会停止看门狗续期；helper 内部自动判断当前线程是否持有，不会误释放他人锁）
            if (locked) {
                redisLockHelper.unlock(lockKey);
            }
        }
    }
}

