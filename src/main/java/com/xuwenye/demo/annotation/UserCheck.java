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

    // 需要匹配的角色编码（如 ROLE_ADMIN / ROLE_SELLER / ROLE_VIP_USER）
    // 为空表示仅校验登录态，不校验角色
    String[] roles() default {};

    // 角色校验失败返回错误码
    int roleErrorCode() default 403;

    // 角色校验失败错误信息
    String roleErrorMessage() default "权限不足";
}
