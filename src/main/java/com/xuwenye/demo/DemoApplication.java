package com.xuwenye.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 项目启动入口
 * <p>
 * @author ZuiM
 */
@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
public class DemoApplication {
    /**
     * 启动 Spring Boot 应用
     * <p>
     * @author ZuiM
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}