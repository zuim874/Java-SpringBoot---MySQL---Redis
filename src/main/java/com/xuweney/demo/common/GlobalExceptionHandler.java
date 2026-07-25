package com.xuweney.demo.common;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 必传参数缺失拦截
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleParamMiss(MissingServletRequestParameterException e) {
        return Result.error(400, "请求参数[" + e.getParameterName() + "]不能为空");
    }

    // 参数校验失败拦截（配合 @Validated 使用）
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleParamValid(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String msg = violations.iterator().next().getMessage();
        return Result.error(400, msg);
    }

    // SQL 语法异常（疑似注入攻击）
    @ExceptionHandler(org.springframework.jdbc.BadSqlGrammarException.class)
    public Result<?> handleSqlErr(Exception e) {
        log.error("疑似 SQL 注入攻击，SQL 语法异常", e);
        return Result.error(400, "参数非法，操作已拦截");
    }

    // 通用异常屏蔽底层堆栈
    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        log.error("未捕获异常", e);  // 打印完整堆栈
        return Result.error(500, "服务器异常，请稍后重试");
    }
}