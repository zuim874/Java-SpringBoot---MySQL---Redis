package com.xuwenye.demo.common;

import com.xuwenye.demo.config.security.SqlInjectSafeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 1.静态资源映射：/uploads/** 指向本地上传目录
 * 2.注册 SQL 注入安全拦截器（排除静态资源）
 * <p>
 * @author ZuiM
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传文件存放根目录（相对项目根目录，与 FileStorageService 保持一致） */
    @Value("${app.upload.dir:./uploads/}")
    private String uploadDir;

    /**
     * 静态资源映射：/uploads/** 映射到本地上传目录，使上传的头像等文件可被浏览器直接访问
     * <p>
     * @author ZuiM
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }

    /**
     * 注册拦截器：SQL 注入安全拦截器拦截所有请求，静态资源除外
     * <p>
     * @author ZuiM
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SqlInjectSafeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/uploads/**");
    }
}
