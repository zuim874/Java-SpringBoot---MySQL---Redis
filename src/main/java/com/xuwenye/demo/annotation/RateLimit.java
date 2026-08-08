package com.xuwenye.demo.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解：标注在 Controller 方法上，由 RateLimitAspect 切面拦截
 * 1.window：时间窗口（秒）
 * 2.maxRequests：窗口内最大请求数
 * 3.message：触发限流时的提示信息
 * <p>
 * @author ZuiM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 时间窗口（秒）
     * <p>
     * @author ZuiM
     * @return int 时间窗口秒数
     */
    int window() default 60;

    /**
     * 窗口内最大请求数
     * <p>
     * @author ZuiM
     * @return int 最大请求数
     */
    int maxRequests() default 100;

    /**
     * 触发限流时的提示信息
     * <p>
     * @author ZuiM
     * @return String 提示信息
     */
    String message() default "操作过于频繁，请稍后再试";
}
