package com.xuwenye.demo.common;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

/**
 * 全局异常处理器：统一捕获并转换为 Result 返回
 * 1.必传参数缺失 → 400
 * 2.参数校验失败（@Validated）→ 400
 * 3.参数非法（如文件类型/大小校验失败）→ 400
 * 4.SQL 语法异常（疑似注入）→ 400
 * 5.其余未知异常 → 500（屏蔽底层堆栈，防止信息泄露）
 * <p>
 * @author ZuiM
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 必传参数缺失拦截
     * <p>
     * @author ZuiM
     * @param e 缺失参数异常
     * @return Result 400 缺失参数提示
     */
    // 必传参数缺失拦截
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleParamMiss(MissingServletRequestParameterException e) {
        return Result.error(400, "请求参数[" + e.getParameterName() + "]不能为空");
    }

    /**
     * 参数校验失败拦截（配合 @Validated 使用）
     * <p>
     * @author ZuiM
     * @param e 参数校验异常
     * @return Result 400 校验提示
     */
    // 参数校验失败拦截（配合 @Validated 使用）
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleParamValid(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String msg = violations.iterator().next().getMessage();
        return Result.error(400, msg);
    }

    /**
     * 参数非法拦截（如文件类型/大小校验失败，抛出业务错误信息）
     * <p>
     * @author ZuiM
     * @param e 非法参数异常
     * @return Result 400 具体错误原因
     */
    // 参数非法拦截（如文件类型/大小校验失败，抛出业务错误信息）
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * SQL 语法异常（疑似注入攻击）
     * <p>
     * @author ZuiM
     * @param e SQL 语法异常
     * @return Result 400 操作拦截提示
     */
    // SQL 语法异常（疑似注入攻击）
    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<?> handleSqlErr(BadSqlGrammarException e) {
        log.error("疑似 SQL 注入攻击，SQL 语法异常", e);
        return Result.error(400, "参数非法，操作已拦截");
    }

    /**
     * 客户端主动断开连接（浏览器关闭/切换页面/取消请求）
     * 属于正常行为，无需记录 ERROR 日志，仅输出 DEBUG 级别
     * <p>
     * @author ZuiM
     * @param e 客户端中断异常
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.debug("客户端断开连接：{}", e.getMessage());
    }

    /**
     * 通用异常屏蔽底层堆栈
     * <p>
     * @author ZuiM
     * @param e 未知异常
     * @return Result 500 服务器异常提示
     */
    // 通用异常屏蔽底层堆栈
    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        log.error("未捕获异常", e);  // 打印完整堆栈
        return Result.error(500, "服务器异常，请稍后重试");
    }
}
