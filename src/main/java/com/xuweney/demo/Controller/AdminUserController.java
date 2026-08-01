package com.xuweney.demo.Controller;

import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口（仅管理员可操作）
 */
@RestController
@RequestMapping("/api/user")
@Validated
public class AdminUserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AdminUserController(UserService userService,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 管理员删除用户（逻辑删除）
     */
    @DeleteMapping("/delete_admin")
    public Result<?> deleteUserByAdmin(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id
    ) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 权限校验：仅管理员可删除
        String loginUsername = jwtUtil.parseUsername(realToken);
        if (!userService.isAdmin(loginUsername)) {
            return Result.error(403, "权限不足，仅管理员可删除用户");
        }

        // 执行删除
        boolean deleteSuccess = userService.deleteById(id);
        if (deleteSuccess) {
            return Result.ok("删除用户成功");
        } else {
            return Result.error(400, "删除失败，该用户不存在");
        }
    }

    /**
     * 管理员恢复用户（逻辑删除恢复）
     */
    @PutMapping("/recover_admin")
    public Result<?> recoverUserByAdmin(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id
    ) {
        // 校验用户是否真的需要恢复
        if (userService.getById(id) != null) {
            return Result.error(400, "当前用户无需恢复");
        }

        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 权限校验：仅管理员可恢复
        String loginUsername = jwtUtil.parseUsername(realToken);
        if (!userService.isAdmin(loginUsername)) {
            return Result.error(403, "权限不足，仅管理员可恢复用户");
        }

        // 执行恢复
        boolean recoverSuccess = userService.recoverById(id);
        if (recoverSuccess) {
            return Result.ok("恢复用户成功");
        } else {
            return Result.error(400, "恢复失败，该用户不存在");
        }
    }
}