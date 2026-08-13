package com.xuwenye.demo.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 1.标注在 Controller 方法上，表示需要记录操作日志
 * 2.通过 AOP 切面拦截，异步发送到 ActiveMQ 队列
 * 3.由 MQConsumer 消费并写入数据库
 * <p>
 * @author ZuiM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作名称（如：用户登录、用户注册、修改密码、用户充值）
     * <p>
     * @author ZuiM
     * @return String 操作名称
     */
    String value();
}