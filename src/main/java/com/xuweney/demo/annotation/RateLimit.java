package com.xuweney.demo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int window() default 60;        // 时间窗口（秒）
    int maxRequests() default 100;  // 最大请求数
    String message() default "操作过于频繁，请稍后再试";
}