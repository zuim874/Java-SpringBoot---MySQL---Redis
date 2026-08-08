package com.xuwenye.demo.Controller;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.FileStorageService;
import com.xuwenye.demo.Service.MQProducer;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import com.xuwenye.demo.util.email.EmailType;
import com.xuwenye.demo.util.email.EmailUtil;
import com.xuwenye.demo.util.oi.SanitizeUtil;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
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
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final FileStorageService fileStorageService;
    private final SanitizeUtil sanitizeUtil;
    private final MQProducer mqProducer;
    public UserController(UserService userService,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          FileStorageService fileStorageService,
                          SanitizeUtil sanitizeUtil,
                          MQProducer mqProducer) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.fileStorageService = fileStorageService;
        this.sanitizeUtil = sanitizeUtil;
        this.mqProducer = mqProducer;
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
     * 1.校验token
     * 2.从token获取当前登录用户
     * 3.校验验证码
     * 4.删除自己（天然不存在越权问题）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param email 当前绑定邮箱
     * @param code 邮箱验证码
     * @return Result<?> 200/400/401/500：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 5, message = "注销尝试过多，请稍后再试")
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
    @RateLimit(window = 60, maxRequests = 5, message = "账号恢复尝试过多，请稍后再试")
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
     * 1.邮箱格式校验
     * 2.检查邮箱是否已注册
     * 3.原子防刷（防止并发数据不一致）
     * 4.发送邮件（消息队列）
     * <p>
     * @author ZuiM
     * @param email 已注册的邮箱
     * @return Result<?> 200/400：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 5, message = "注销邮箱发送过多，请稍后再试")
    @PostMapping("/send-deletecode")
    public Result<?> sendDeleteCode(@RequestParam String email) {
        if (deleteEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                String realEmail = sanitizeUtil.dealEmail(email);
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }
                // 2. 原子防刷：首次设置限流 key 成功才放行（60 秒内重复请求被拦截）
                String sendLimitKey = "verify_deleteCode_limit:" + realEmail;
                Boolean firstRequest = redisUtil.setIfAbsent(sendLimitKey, "1");
                if (Boolean.FALSE.equals(firstRequest)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }
                // 3. 发送邮件（消息队列）
//                emailUtil.sendVerificationCode(email, 2);
                mqProducer.sendEmailTask(email, 2);
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
     * 1.邮箱格式校验
     * 2.检查邮箱是否已注册
     * 3.原子防刷（防止并发数据不一致）
     * 4.发送邮件（消息队列）
     * <p>
     * @author ZuiM
     * @param email 已注册的邮箱
     * @return Result<?> 200/400：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 5, message = "恢复邮箱发送过多，请稍后再试")
    @PostMapping("/send-recovercode")
    public Result<?> sendRecoverCode(@RequestParam String email) {
        if (recoverEmailCheckStatus) {
            try {
                // 0. 邮箱格式校验
                if (!EmailUtil.isValidEmail(email)) {
                    return Result.error(400, "邮箱格式不正确");
                }
                String realEmail = sanitizeUtil.dealEmail(email);
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }
                // 2. 原子操作
                String sendLimitKey = "verify_recoverCode_limit:" + realEmail;
                Boolean success = redisUtil.setIfAbsent(sendLimitKey, "1");
                if (Boolean.FALSE.equals(success)) {
                    long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
                    return Result.error(400, "请等待 " + ttl + " 秒后再试");
                }
                // 3. 发送邮件（异步发送，消息队列）
//                emailUtil.sendVerificationCode(email, 1);
                mqProducer.sendEmailTask(email, 1);
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
     * 1.校验token
     * 2.查询当前用户
     * 3.清空敏感字段再返回（密码、逻辑删除标记不外泄）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @return Result<?> 200/400/401：成功/失败
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
     * 1.校验token
     * 2.查询当前用户
     * 3.保存文件（类型/大小校验在 FileStorageService 内完成）
     * 4.更新数据库并清理缓存
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param file 头像图片文件（jpg/png/gif/webp，大小不超过 2MB）
     * @return Result<?> 200/400/401/500：成功返回新头像 URL
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

    /**
     * 更新用户资料（仅昵称，邮箱换绑请调用独立的 /change_email 接口）
     * 1.校验token
     * 2.获取当前登录用户
     * 3.校验昵称（非空、长度 1~20）
     * 4.更新昵称（不涉及邮箱字段）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param nickname 新昵称
     * @return Result<?> 200/400/401/500：成功/失败
     */
    @RateLimit(window = 60, maxRequests = 5, message = "资料更新过于频繁，请稍后再试")
    @PutMapping("/update_profile")
    public Result<?> updateProfile(@RequestHeader("Authorization") String token,
                                   @RequestParam String nickname) {
        // 1. 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "当前未登录，请先登录！");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "登录信息已过期，请重新登录");
        }

        // 2. 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User currentUser = userService.findUsernameforlogin(loginUsername);
        if (currentUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 3. 校验昵称（非空、长度 1~20）
        if (nickname == null || nickname.trim().isEmpty()) {
            return Result.error(400, "昵称不能为空");
        }
        String realNickname = nickname.trim();
        if (realNickname.length() > 20) {
            return Result.error(400, "昵称长度不能超过 20 个字符");
        }

        // 4. 更新昵称（不涉及邮箱字段）
        boolean updated = userService.updateProfile(currentUser.getId(), realNickname, null);
        if (!updated) {
            return Result.error(500, "资料更新失败，请稍后重试");
        }

        return Result.ok("资料更新成功");
    }

    /**
     * 换绑邮箱（独立接口，不归属资料更新）
     * 1.校验token
     * 2.获取当前登录用户
     * 3.统一邮箱格式（转为小写）
     * 4.新邮箱不能与当前绑定邮箱相同
     * 5.旧邮箱必须与当前绑定邮箱一致
     * 6.邮箱格式校验
     * 7.邮箱不能已被绑定
     * 8.校验新旧邮箱验证码
     * 9.更新邮箱
     * 10.清除redis缓存验证码
     * <p>
     * @author ZuiM
     * @param oldEmail 当前绑定邮箱
     * @param oldEmail_code 当前邮箱验证码
     * @param newEmail 新邮箱
     * @param newEmail_code 新邮箱验证码
     * 需先调用 /send-changeEmail 获取两组验证码：
     * type=3 旧邮箱验证码（发到旧邮箱）、type=4 新邮箱验证码（发到新邮箱）
     */
    @RateLimit(window = 60, maxRequests = 5, message = "邮箱更换过于频繁，请稍后再试")
    @PutMapping("/change_email")
    public Result<?> changeEmail(@RequestHeader("Authorization") String token,
                                 @RequestParam String oldEmail,
                                 @RequestParam String oldEmail_code,
                                 @RequestParam String newEmail,
                                 @RequestParam String newEmail_code) {
        // 1. 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "当前未登录，请先登录！");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "登录信息已过期，请重新登录");
        }

        // 2. 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User currentUser = userService.findUsernameforlogin(loginUsername);
        if (currentUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 3. 统一邮箱格式（与 send-changeEmail 发送验证码时的处理保持一致，避免大小写导致 key 对不上）
        String realOldEmail = sanitizeUtil.dealEmail(oldEmail.trim());
        String realNewEmail = sanitizeUtil.dealEmail(newEmail.trim());
        String realNowEmail = sanitizeUtil.dealEmail(currentUser.getEmail());

        // 4. 新邮箱不能与当前相同
        if (realNewEmail.equals(realNowEmail)) {
            return Result.error(400, "新邮箱与当前邮箱相同，无需换绑");
        }
        // 5. 旧邮箱必须与当前绑定邮箱一致（防止使用他人邮箱完成验证）
        if (!realOldEmail.equals(realNowEmail)) {
            return Result.error(400, "旧邮箱与当前绑定邮箱不一致");
        }
        // 6. 新邮箱格式校验
        if (!EmailUtil.isValidEmail(realNewEmail)) {
            return Result.error(400, "新邮箱格式不正确");
        }
        // 7. 新邮箱不能已被其他账号使用
        if (userService.isEmailExist(realNewEmail)) {
            return Result.error(400, "该新邮箱已被注册");
        }

        // 8. 校验旧邮箱验证码（verify_oldEmailCheckCode:{旧邮箱}）
        if (changeEmailCodeCheckStatus) {
            String oldRedisKey = "verify_oldEmailCheckCode:" + realOldEmail;
            String oldStoredCode = (String) redisUtil.get(oldRedisKey);
            if (oldStoredCode == null) {
                return Result.error(400, "旧邮箱验证码已过期，请重新获取");
            }
            if (!oldStoredCode.equals(oldEmail_code)) {
                return Result.error(400, "旧邮箱验证码错误");
            }

            // 9. 校验新邮箱验证码（verify_newEmailCheckCode:{新邮箱}）
            String newRedisKey = "verify_newEmailCheckCode:" + realNewEmail;
            String newStoredCode = (String) redisUtil.get(newRedisKey);
            if (newStoredCode == null) {
                return Result.error(400, "新邮箱验证码已过期，请重新获取");
            }
            if (!newStoredCode.equals(newEmail_code)) {
                return Result.error(400, "新邮箱验证码错误");
            }
        }

        // 10. 更新邮箱（不涉及昵称字段）
        boolean updated = userService.updateProfile(currentUser.getId(), null, newEmail);
        if (!updated) {
            return Result.error(500, "换绑邮箱失败，请稍后重试");
        }

        // 11. 换绑成功后清除验证码，防止重复使用（用统一后的邮箱保证 key 一致）
        redisUtil.delete("verify_oldEmailCheckCode:" + realOldEmail);
        redisUtil.delete("verify_newEmailCheckCode:" + realNewEmail);

        return Result.ok("邮箱更换成功");
    }

    /**
     * 发送换绑邮箱验证码
     * 1.校验token
     * 2.获取当前登录用户
     * 3.校验邮件类型（仅支持旧邮箱/新邮箱验证）
     * 4.邮箱格式校验（统一转换为小写）
     * 5.业务校验（旧邮箱是否为当前当前绑定邮箱，新邮箱是否已经绑定）
     * 6.原子防刷（防止并发数据不一致）
     * 7.发送邮件
     * <p>
     * @author ZuiM
     * @param email 收件邮箱
     * @param type 邮件类型
     * type=3（EmailType.ChangeOld）：发送到旧邮箱，email 必须与当前绑定邮箱一致
     * type=4（EmailType.ChangeNew）：发送到新邮箱，email 必须未被其他账号使用
     */
    @RateLimit(window = 60, maxRequests = 5, message = "更改邮箱验证码发送过于频繁，请稍后重试！")
    @PostMapping("/send-changeEmail")
    public Result<?> sendChangeEmail(@RequestHeader("Authorization") String token,
                                     @RequestParam String email,
                                     @RequestParam Integer type) {
        // 1. 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "当前未登录，请先登录！");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "登录状态已过期，请重新登录");
        }

        // 2. 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User currentUser = userService.findUsernameforlogin(loginUsername);
        if (currentUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 3. 校验邮件类型（仅支持旧邮箱验证 / 新邮箱验证两种）
        if (type == null
                || (!type.equals(EmailType.ChangeOld.getCode()) && !type.equals(EmailType.ChangeNew.getCode()))) {
            return Result.error(400, "邮件类型不正确");
        }

        // 4. 邮箱格式校验 + 统一小写
        if (!EmailUtil.isValidEmail(email)) {
            return Result.error(400, "邮箱格式不正确");
        }
        String realEmail = sanitizeUtil.dealEmail(email);

        // 5. 业务校验（前置，避免无效请求占用限流窗口）
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

        // 6. 原子防刷：首次设置限流 key 成功才放行（同一邮箱同一类型 5 分钟内不重复发送）
        String sendLimitKey = "verify_changeEmailCode_limit:" + type + ":" + realEmail;
        Boolean firstRequest = redisUtil.setIfAbsent(sendLimitKey, "1");
        if (Boolean.FALSE.equals(firstRequest)) {
            long ttl = redisUtil.getExpire(sendLimitKey, TimeUnit.SECONDS);
            return Result.error(400, "请等待 " + ttl + " 秒后再试");
        }

        // 7. 发送邮件（消息队列，EmailType 3=旧邮箱验证、4=新邮箱验证）
        mqProducer.sendEmailTask(realEmail, type);
        return Result.ok("换绑邮箱验证码已发送到您的邮箱，请注意查收");
    }

    @RateLimit(window = 60, maxRequests = 5, message = "更改密码过于频繁，请稍后重试！")
    @PostMapping("/change_password")
    public Result<?> changePassword(@RequestHeader("Authorization") String token,
                                    @RequestParam String oldPassword,
                                    @RequestParam String newPassword,
                                    @RequestParam String newPassword_check) {
        return Result.ok("密码修改成功");
    }
}

