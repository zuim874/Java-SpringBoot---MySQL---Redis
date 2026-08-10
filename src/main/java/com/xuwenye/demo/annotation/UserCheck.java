package com.xuwenye.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserCheck {

    // 是否必须检验用户身份（默认true）
    boolean checkUser() default true;

    // 校验失败返回错误码
    int errorCode() default 401;

    // 错误信息
    String errorMessage() default "未登录或登录信息已过期";
}
