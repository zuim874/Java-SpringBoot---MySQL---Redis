package com.xuwenye.demo.aspect;

import com.xuwenye.demo.annotation.RateLimit;
import com.xuwenye.demo.common.Result;
import com.xuwenye.demo.util.redis.RedisUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面：拦截标注 @RateLimit 的方法，基于 Redis 计数实现滑动窗口限流
 * 1.获取注解配置（窗口/最大请求数/提示信息）
 * 2.生成限流 key（IP + 类名 + 方法名）
 * 3.调用 Redis 原子自增（Lua：自增 + 首次设置过期）
 * 4.超过阈值返回 429，否则放行
 * <p>
 * @author ZuiM
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RedisUtil redisUtil;
    private static final String PREFIX = "rate_limit:";

    public RateLimitAspect(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 环绕通知：执行限流检查后放行原方法
     * 1.获取注解
     * 2.生成 Key
     * 3.限流检查（Lua 原子操作：自增 + 首次设置过期时间，避免 key 永久残留）
     * 4.执行原方法
     * <p>
     * @author ZuiM
     * @param joinPoint 连接点
     * @return Object 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(com.xuwenye.demo.annotation.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 2. 生成 Key
        String key = getKey(joinPoint);

        // 3. 限流检查（Lua 原子操作：自增 + 首次设置过期时间，避免 key 永久残留）
        String redisKey = PREFIX + key;
        Long count = redisUtil.incrementAndExpire(redisKey, rateLimit.window(), TimeUnit.SECONDS);

        // Redis 异常时 count 为 null，放行（限流降级，保证业务可用性）
        if (count != null && count > rateLimit.maxRequests()) {
            return Result.error(429, rateLimit.message());
        }

        // 4. 执行原方法
        return joinPoint.proceed();
    }

    /**
     * 生成限流 Key：真实客户端 IP + 类名 + 方法名
     * 1.优先取 X-Forwarded-For 头（nginx 等反向代理会写入），取第一个 IP 即客户端真实 IP
     * 2.无代理时（直连）退化为 getRemoteAddr()
     * 3.取接口声明类型名与方法名（避免代理类名变化导致 key 漂移）
     * <p>
     * 说明：若直接使用 getRemoteAddr()，经 nginx 代理后所有请求的 IP 都是代理服务器
     * （如 127.0.0.1），会导致所有用户共享同一个限流桶、互相挤占额度（表现为偶发 429）。
     * <p>
     * @author ZuiM
     * @param joinPoint 连接点
     * @return String 限流 key（如 1.2.3.4:xxx.AuthController:login）
     */
    private String getKey(ProceedingJoinPoint joinPoint) {
        // 获取 IP
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = "unknown";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 优先取反向代理写入的 X-Forwarded-For（真实客户端 IP）
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                // 直连场景：无代理头，退化为连接来源 IP
                ip = request.getRemoteAddr();
            } else {
                // 多级代理时 X-Forwarded-For 形如 "client, proxy1, proxy2"，取第一个
                ip = ip.split(",")[0].trim();
            }
        }

        // 获取类名和方法名（用接口声明的类型名，避免代理类名变化导致 key 漂移）
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        return ip + ":" + className + ":" + methodName;
    }
}