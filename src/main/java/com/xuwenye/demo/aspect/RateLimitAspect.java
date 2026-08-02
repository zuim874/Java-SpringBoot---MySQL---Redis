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

@Aspect
@Component
public class RateLimitAspect {

    private final RedisUtil redisUtil;
    private static final String PREFIX = "rate_limit:";

    public RateLimitAspect(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

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

    private String getKey(ProceedingJoinPoint joinPoint) {
        // 获取 IP
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = "unknown";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = request.getRemoteAddr();
        }

        // 获取类名和方法名（用接口声明的类型名，避免代理类名变化导致 key 漂移）
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        return ip + ":" + className + ":" + methodName;
    }
}