package com.xuwenye.demo.Controller.Admin;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.common.Result;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口（仅管理员可操作）
 * 1.删除用户（逻辑删除，需管理员权限码）
 * 2.恢复用户（逻辑删除恢复，需管理员权限码）
 * 3.登录态与管理员角色校验由 @UserCheck 切面完成
 * <p>
 * @author ZuiM
 */
@RestController
@RequestMapping("/api/user")
@Validated
public class AdminUserController {


    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Value("${test.adminCheckCode}")
    private String adminCheckCode;

    /**
     * 管理员删除用户（逻辑删除）
     * 1.@UserCheck 切面校验管理员身份
     * 2.校验管理员权限操作码
     * 3.执行逻辑删除（清理缓存）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入，需 ROLE_ADMIN 角色）
     * @param id 目标用户 ID
     * @param adminCode 管理员权限操作码
     * @return Result 200 删除成功；400 用户不存在；403 权限不足
     */
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @DeleteMapping("/delete_admin")
    public Result<?> deleteUserByAdmin(
            User currentUser,
            @Min(1) @RequestParam Long id,
            @RequestParam String adminCode
    ) {
        // 校验管理员权限操作码（先判空，避免 null.equals 抛 NPE）
        if (adminCode == null || !adminCode.equals(adminCheckCode)) {
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
     * 1.@UserCheck 切面校验管理员身份
     * 2.校验管理员权限操作码
     * 3.执行恢复（刷新缓存）
     * <p>
     * @author ZuiM
     * @param currentUser 当前登录用户（切面注入，需 ROLE_ADMIN 角色）
     * @param id 目标用户 ID
     * @param adminCode 管理员权限操作码
     * @return Result 200 恢复成功；400 用户不存在；403 权限不足
     */
    @UserCheck(roles = {"ROLE_ADMIN"}, roleErrorMessage = "权限不足，仅管理员可操作")
    @PutMapping("/recover_admin")
    public Result<?> recoverUserByAdmin(
            User currentUser,
            @Min(1) @RequestParam Long id,
            @RequestParam String adminCode
    ) {
        // 校验管理员权限操作码（先判空，避免 null.equals 抛 NPE）
        if (adminCode == null || !adminCode.equals(adminCheckCode)) {
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
}