package com.xuwenye.demo.config.MyBatisPlus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置
 * 1.注册 MybatisPlusInterceptor 拦截器
 * 2.添加分页插件（PaginationInnerInterceptor），指定数据库类型为 MySQL
 * <p>
 * @author ZuiM
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 插件容器
     * 1.创建拦截器实例
     * 2.添加分页插件（指定 MySQL 方言）
     * <p>
     * @author ZuiM
     * @return MybatisPlusInterceptor 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页拦截器，数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}