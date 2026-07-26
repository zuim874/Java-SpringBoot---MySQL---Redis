package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.JwtUtil;
import com.xuweney.demo.util.RedisUtil;
import com.xuweney.demo.util.EmailUtil;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {
    @Value("${test.recoverCodeCheck.status:true}")
    private Boolean recoverCodeCheckStatus;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;

    public UserController(UserService userService,
                          JwtUtil jwtUtil,
                          RedisUtil redisUtil,
                          EmailUtil emailUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.emailUtil = emailUtil;
    }

    @DeleteMapping("/delete_admin")
    public Result<?> deleteUserByAdminParam(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id
    ){
        // 第一步：剥离Bearer前缀，校验token合法性
        if(token == null || !token.startsWith("Bearer ")){
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if(!jwtUtil.validate(realToken)){
            return Result.error(401, "令牌失效，请重新登录");
        }
        // 获取当前登录用户名，用于做权限判断（仅管理员可删除）
        String loginUsername = jwtUtil.parseUsername(realToken);
        System.out.println(loginUsername);
        if(!userService.is_admin(loginUsername)){
            return Result.error(403, "权限不足，仅管理员可删除用户");
        }

        // 第二步：调用业务层执行删除
        boolean deleteSuccess = userService.deleteById(id);
        if(deleteSuccess){
            return Result.ok("删除用户成功");
        }else{
            return Result.error(400, "删除失败，该用户不存在");
        }
    }

    @DeleteMapping("/delete_user")
    public Result<?> deleteUserByUserParam(
            @RequestHeader("Authorization") String token
            // 移除@RequestParam Long id 参数
    ){
        // 1、校验登录token
        if(token == null || !token.startsWith("Bearer ")){
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if(!jwtUtil.validate(realToken)){
            return Result.error(401, "令牌失效，请重新登录");
        }
        // 2、直接从token获取当前登录用户名，无需前端传id
        String loginUsername = jwtUtil.parseUsername(realToken);
        // 3、根据用户名查询当前登录用户
        User targetUser = userService.findUsernameforlogin(loginUsername);
        if(targetUser == null){
            return Result.error(400, "用户不存在");
        }
        // 4、直接删除当前登录用户，天然不存在越权问题（只能删自己）
        boolean deleteSuccess = userService.deleteById(targetUser.getId());
        if(deleteSuccess){
            return Result.ok("账号注销成功");
        }else{
            return Result.error(500, "账号注销失败，请稍后重试");
        }
    }

    @PutMapping("/recover_admin")
    public Result<?> recoverUserByAdminParam(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id
    ){
        // 前提：检验用户是否真的需要恢复
        if(userService.getById(id) != null) {
            return Result.error(400, "当前用户无需恢复");
        }
        // 第一步：剥离Bearer前缀，校验token合法性
        if(token == null || !token.startsWith("Bearer ")){
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if(!jwtUtil.validate(realToken)){
            return Result.error(401, "令牌失效，请重新登录");
        }
        // 获取当前登录用户权限
        String loginUsername = jwtUtil.parseUsername(realToken);
        if(!userService.is_admin(loginUsername)){
            return Result.error(403, "权限不足，仅管理员可删除用户");
        }

        // 第二步：调用业务层执行恢复
        boolean recoverSuccess = userService.recoverById(id);
        if(recoverSuccess){
            return Result.ok("恢复用户成功");
        }else{
            return Result.error(400, "恢复失败，该用户不存在");
        }
    }

    @PutMapping("/recover_user") // 当前逻辑异常：若要恢复说明已删除，已删除则不可登录（需引入新验证方式）
    public Result<?> recoverUserByUserParam(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String code
    ) {
        // 1:检查用户名
        User user = userService.findUsernameforRecover(username);
        if (user == null) {
            return Result.error(400,"请检查用户名是否正确");
        }
        // 2:检查email
        if (!user.getEmail().equals(email)) {
            return Result.error(400, "请检查绑定的邮箱是否正确");
        }

        String redisKey = "verify_recoverCode:" + email;
        String storedCode = (String) redisUtil.get(redisKey);
        // 3:发送验证码并验证其正确
        if (recoverCodeCheckStatus) {
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

        // 4:调用业务层执行恢复
        boolean recoverSuccess = userService.recoverById(user.getId());
        if(recoverSuccess){
            return Result.ok("恢复用户成功");
        }else{
            return Result.error(400, "恢复失败，该用户不存在");
        }

    }

    @PostMapping("/send-recovercode")
    public Result<?> sendVerificationCode(@RequestParam String email) {
        if (recoverCodeCheckStatus) {
            try {
                // 1. 检查邮箱是否已注册
                if (!userService.isEmailExist(email)) {
                    return Result.error(400, "该邮箱未绑定账号");
                }

                // 2. 检查是否频繁发送（防刷）
                String redisKey = "verify_recoverCode:" + email;
                String sendLimitKey = "verify_recoverCode_limit:" + email;

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
                emailUtil.sendRecoverAccountCode(email, code);

                return Result.ok("账号恢复验证码已发送到您的邮箱，请注意查收");

            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(400, "发送验证码失败：" + e.getMessage());
            }
        }
        else {
            return Result.ok("已跳过账号恢复邮箱验证");
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
