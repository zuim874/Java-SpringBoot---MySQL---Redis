package com.xuwenye.demo.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuwenye.demo.Entity.User;
import com.xuwenye.demo.Service.MQProducer;
import com.xuwenye.demo.Service.UserService;
import com.xuwenye.demo.annotation.OperationLog;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.auth.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面：配合 @OperationLog 注解，自动记录用户操作日志
 * 1.拦截所有标注 @OperationLog 的 Controller 方法
 * 2.提取请求信息（URL、IP、参数、耗时）
 * 3.提取当前用户信息（从 Token 解析）
 * 4.将日志数据通过 ActiveMQ 异步发送到日志队列
 * 5.日志记录不阻塞主业务流程
 * <p>
 * @author ZuiM
 */
@Aspect
@Component
public class OperationLogAspect {

    private final MQProducer mqProducer;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    public OperationLogAspect(MQProducer mqProducer, JwtUtil jwtUtil, ObjectMapper objectMapper, UserService userService) {
        this.mqProducer = mqProducer;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    /**
     * 环绕通知：拦截 @OperationLog 方法，记录操作日志
     * 1.获取注解配置的操作名称
     * 2.提取请求信息（URL、IP、参数）
     * 3.提取当前用户信息
     * 4.执行原方法并捕获结果
     * 5.统计执行耗时
     * 6.通过 ActiveMQ 异步发送日志消息
     * <p>
     * @author ZuiM
     * @param joinPoint 连接点
     * @return Object 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(com.xuwenye.demo.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解配置的操作名称
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        String operationName = operationLog.value();

        // 2. 提取请求信息
        HttpServletRequest request = getRequest();
        String requestUrl = (request != null) ? request.getRequestURI() : "";
        String requestIp = (request != null) ? getClientIp(request) : "";
        String requestParams = extractParams(joinPoint);

        // 3. 提取当前用户信息
        String username = "";
        Long userId = null;
        if (request != null) {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                String realToken = token.substring(7);
                if (jwtUtil.validate(realToken)) {
                    username = jwtUtil.parseUsername(realToken);
                    // Token 中仅包含用户名，通过查询数据库获取 userId
                    if (!username.isEmpty()) {
                        User user = userService.findUserableUser(username);
                        if (user != null) {
                            userId = user.getId();
                        }
                    }
                }
            }
        }

        // 4. 获取方法全限定名
        String methodName = joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();

        // 5. 执行原方法并记录耗时
        long startTime = System.currentTimeMillis();
        Object result;
        boolean success = true;
        String errorMsg = null;
        try {
            result = joinPoint.proceed();
            // 判断返回结果是否标记失败（Result 的 code 非 200 即失败）
            // 注意：不能依赖 toString() 判断，Result 未重写 toString，默认输出为对象地址
            if (result instanceof Result<?> res && res.getCode() != 200) {
                success = false;
                errorMsg = res.getMes();
            }
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            throw e; // 继续抛出异常，不影响业务
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 6. 组装日志数据并通过 ActiveMQ 异步发送
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("username", username);
            logData.put("operation", operationName);
            logData.put("method", methodName);
            logData.put("requestUrl", requestUrl);
            logData.put("requestIp", requestIp);
            logData.put("requestParams", requestParams);
            logData.put("status", success ? 1 : 0);
            logData.put("errorMsg", errorMsg);
            logData.put("duration", duration);

            mqProducer.sendLogTask(logData);
        }

        return result;
    }

    /**
     * 获取当前请求对象
     * <p>
     * @author ZuiM
     * @return HttpServletRequest 当前请求（可能为null）
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * @author ZuiM
     * @param request 当前请求
     * @return String 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 提取请求参数（JSON 格式）
     * 1.排除 User 类型参数（避免序列化敏感信息）
     * 2.排除密码等敏感字段
     * <p>
     * @author ZuiM
     * @param joinPoint 连接点
     * @return String 参数 JSON 字符串
     */
    private String extractParams(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            Map<String, Object> params = new HashMap<>();

            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    // 跳过 User 类型参数（已包含用户信息，由注解单独处理）
                    if (args[i] instanceof User) {
                        continue;
                    }
                    // 跳过密码等敏感字段
                    String paramName = paramNames[i].toLowerCase();
                    if (paramName.contains("password") || paramName.contains("pwd") ||
                            paramName.contains("secret") || paramName.contains("token")) {
                        params.put(paramNames[i], "******");
                        continue;
                    }
                    params.put(paramNames[i], args[i]);
                }
            }
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }
}