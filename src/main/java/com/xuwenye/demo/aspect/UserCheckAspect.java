package com.xuwenye.demo.aspect;

import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.UserCheck;
import com.xuwenye.demo.util.auth.JwtUtil;
import com.xuwenye.demo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 用户身份校验切面：配合 @UserCheck 注解，统一完成「登录态校验 + 当前用户查询注入」
 * <p>
 * 1.校验请求头 Authorization 中的 Bearer Token（未登录/无效 → 401）
 * 2.解析用户名并查询当前登录用户（用户不存在 → 按注解配置返回错误）
 * 3.将 currentUser 注入到方法参数（方法需声明一个 User 类型参数接收，如：
 *    public Result&lt;?&gt; xxx(User currentUser, @RequestParam ...)）
 * 4.注入后执行原方法——Controller 中不再需要重复的 token 校验 / 查用户样板代码
 * <p>
 * 注意：User 类型参数无需（也不应）添加 @RequestParam/@RequestBody 等绑定注解，
 * 由切面直接替换参数值注入；Spring MVC 对无注解复杂类型的 ModelAttribute 绑定
 * 结果会被切面注入覆盖，业务上无影响。
 * <p>
 * @author ZuiM
 */
@Aspect
@Component
public class UserCheckAspect {
    private final JwtUtil jwtUtil;
    private final UserService userService;
    public UserCheckAspect(JwtUtil jwtUtil,
                           UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    /**
     * 环绕通知：校验登录态并注入当前用户
     * 1.获取注解配置
     * 2.校验 Authorization 头（未携带 / 格式错误 / 令牌失效 → 401）
     * 3.查询当前登录用户（不存在 → 按注解 errorCode/errorMessage 返回）
     * 4.按注解 checkUser 决定是否注入（checkUser=false 时仅校验 Token，不注入）
     * 5.将当前用户注入到方法参数后执行原方法
     * <p>
     * @author ZuiM
     * @param joinPoint 连接点
     * @return Object 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(com.xuwenye.demo.annotation.UserCheck)")
    public Object checkUser(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解配置
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        UserCheck userCheck = method.getAnnotation(UserCheck.class);
        if (userCheck == null) {
            return joinPoint.proceed();
        }

        // 2. 获取请求对象
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Result.error(500, "系统错误");
        }
        HttpServletRequest request = attributes.getRequest();

        // 3. 校验 Token（未携带 / 非 Bearer 格式 / 令牌失效 → 401）
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error(401, "未登录，请先登录");
        }
        String realToken = token.substring(7);
        if (!jwtUtil.validate(realToken)) {
            return Result.error(401, "令牌失效，请重新登录");
        }

        // 4. 是否需要查询并注入当前用户
        if (!userCheck.checkUser()) {
            // 仅校验登录态，不注入用户（方法自行处理用户逻辑）
            return joinPoint.proceed();
        }

        // 5. 查询当前登录用户
        String loginUsername = jwtUtil.parseUsername(realToken);
        User currentUser = userService.findUserableUser(loginUsername);
        if (currentUser == null) {
            return Result.error(userCheck.errorCode(), userCheck.errorMessage());
        }

        // 6. 注入到方法参数（查找 User 类型参数）
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        boolean injected = false;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getType() == User.class) {
                args[i] = currentUser;
                injected = true;
                break;
            }
        }
        if (!injected) {
            // 方法声明了 @UserCheck 却未提供 User 类型参数接收注入，属于编码遗漏
            return Result.error(500, "@UserCheck 方法缺少 User 类型参数，无法注入当前用户");
        }

        // 7. 执行原方法（携带注入后的参数）
        return joinPoint.proceed(args);
    }
}
