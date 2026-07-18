package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.JwtUtil;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     *
     * @param token
     * @param id
     * @return
     */
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
        if(!"admin".equals(loginUsername)){
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

    /**
     *
     * @param token
     * @return
     */
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
}
