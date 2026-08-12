package com.xuwenye.demo.Controller.Admin;

import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口（仅管理员可操作）
 * 1.删除用户（逻辑删除，需管理员权限码）
 * 2.恢复用户（逻辑删除恢复，需管理员权限码）
 * <p>
 * @author ZuiM
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

    @Value("${test.adminCheckCode}")
    private String adminCheckCode;

    /**
     * 管理员删除用户（逻辑删除）
     * 1.校验管理员身份与权限码
     * 2.执行逻辑删除（清理缓存）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param id 目标用户 ID
     * @param adminCode 管理员权限操作码
     * @return Result 200 删除成功；400 用户不存在；403 权限不足
     */
    @DeleteMapping("/delete_admin")
    public Result<?> deleteUserByAdmin(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id,
            @RequestParam String adminCode
    ) {
        if (!adminCheck(token,adminCode)) {
            return Result.error(403, "权限不足或管理员权限操作码错误");
        }

        // 执行删除
        boolean deleteSuccess = userService.deleteUserById(id);
        if (deleteSuccess) {
            return Result.ok("删除用户成功");
        } else {
            return Result.error(400, "删除失败，该用户不存在");
        }
    }

    /**
     * 管理员恢复用户（逻辑删除恢复）
     * 1.校验管理员身份与权限码
     * 2.执行恢复（刷新缓存）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param id 目标用户 ID
     * @param adminCode 管理员权限操作码
     * @return Result 200 恢复成功；400 用户不存在；403 权限不足
     */
    @PutMapping("/recover_admin")
    public Result<?> recoverUserByAdmin(
            @RequestHeader("Authorization") String token,
            @Min(1) @RequestParam Long id,
            @RequestParam String adminCode
    ) {
        if (!adminCheck(token,adminCode)) {
            return Result.error(403, "权限不足或管理员权限操作码错误");
        }

        // 执行恢复
        boolean recoverSuccess = userService.recoverUserById(id);
        if (recoverSuccess) {
            return Result.ok("恢复用户成功");
        } else {
            return Result.error(400, "恢复失败，该用户不存在");
        }
    }

    /**
     * 管理员权限校验
     * 1.校验 token 有效性
     * 2.校验用户角色为 ROLE_ADMIN
     * 3.校验管理员权限操作码（先判空，避免 null.equals 抛 NPE）
     * <p>
     * @author ZuiM
     * @param token 登录令牌（Bearer xxx）
     * @param adminCode 管理员权限操作码
     * @return boolean true=通过校验
     */
    private boolean adminCheck(String token,
                               String adminCode) {
        // 校验 token
        if (token == null || !token.startsWith("Bearer ")) {
            return false;
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return false;
        }
        // 权限校验：仅管理员可删除
        String loginUsername = jwtUtil.parseUsername(realToken);
        if (!userService.isAdmin(loginUsername)) {
            return false;
        }
        // 管理员权限码确认（先判空，避免 null.equals 抛 NPE）
        if (adminCode == null || !adminCode.equals(adminCheckCode)) {
            return false;
        }

        return true;
    }
}