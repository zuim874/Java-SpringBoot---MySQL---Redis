package com.xuwenye.demo.common;

import com.xuwenye.demo.config.security.SqlInjectSafeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传文件存放根目录（相对项目根目录，与 FileStorageService 保持一致） */
    @Value("${app.upload.dir:./uploads/}")
    private String uploadDir;

    /**
     * 静态资源映射：/uploads/** 映射到本地上传目录，使上传的头像等文件可被浏览器直接访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SqlInjectSafeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/uploads/**");
    }
}
