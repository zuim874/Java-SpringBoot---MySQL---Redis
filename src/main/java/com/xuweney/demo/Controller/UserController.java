package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户自助操作接口（用户操作自己的账号）
 */
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注销自己的账号（逻辑删除）
     */
    @DeleteMapping("/delete_user")
    public Result<?> deleteSelf(
            @RequestHeader("Authorization") String token
    ) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 从 token 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User targetUser = userService.findUsernameforlogin(loginUsername);
        if (targetUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 删除自己（天然不存在越权问题）
        boolean deleteSuccess = userService.deleteById(targetUser.getId());
        if (deleteSuccess) {
            return Result.ok("账号注销成功");
        } else {
            return Result.error(500, "账号注销失败，请稍后重试");
        }
    }

    /**
     * 用户恢复自己的账号
     */
    @PutMapping("/recover_user")
    public Result<?> recoverSelf(
            @RequestHeader("Authorization") String token
    ) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 从 token 获取当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User targetUser = userService.findUsernameforlogin(loginUsername);
        if (targetUser == null) {
            return Result.error(400, "用户不存在");
        }

        // 恢复自己
        boolean recoverSuccess = userService.recoverById(targetUser.getId());
        if (recoverSuccess) {
            return Result.ok("账号恢复成功");
        } else {
            return Result.error(400, "账号恢复失败，请稍后重试");
        }
    }
}