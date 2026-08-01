package com.xuweney.demo.common;

import com.xuweney.demo.config.security.SqlInjectSafeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SqlInjectSafeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**");
    }
}