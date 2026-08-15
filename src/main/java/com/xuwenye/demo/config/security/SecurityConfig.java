package com.xuwenye.demo.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Spring Security 安全配置
 * 1.配置 BCrypt 密码编码器
 * 2.配置安全过滤链（无状态会话、放行公开接口、注册 JWT 过滤器）
 * 3.配置 CORS 跨域
 * <p>
 * @author ZuiM
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器（BCrypt 加密）
     * <p>
     * @author ZuiM
     * @return PasswordEncoder 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链
     * 1.开启 CORS、关闭 CSRF
     * 2.设置无状态会话（STATELESS）
     * 3.放行公开接口（登录/注册/发送验证码/恢复账号/商品浏览(GET)/静态资源 /uploads/**）
     * 4.其余请求要求认证，并在 UsernamePasswordAuthenticationFilter 前注册 JWT 过滤器
     * 5.未认证统一返回 401 JSON（便于前端拦截处理）
     * <p>
     * @author ZuiM
     * @param http HttpSecurity 安全构建器
     * @return SecurityFilterChain 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/send-registercode",
                                "/api/user/send-recovercode", "/api/user/recover_user", "/uploads/**").permitAll()
                        // 商品公开接口（无需登录）
                        .requestMatchers("/api/product/page", "/api/product/detail/**", "/api/product/categories",
                                "/api/product/list", "/api/product/list/category",
                                "/api/product/sellers").permitAll()
                        // 卖家详情公开接口（GET请求）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/product/seller/**").permitAll()
                        // 领券中心可领模板公开接口（GET请求，无需登录）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/coupon/user/templates").permitAll()
                        .anyRequest().authenticated()
                )
                // 未认证访问返回 401 JSON（避免 Spring Security 默认 403 空响应导致前端 JSON 解析报错）
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"mes\":\"未登录或登录已过期\"}");
                }))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域配置（允许所有来源、常用方法、携带凭证）
     * 1.允许所有来源（AllowedOriginPatterns）
     * 2.允许 GET/POST/PUT/DELETE/OPTIONS
     * 3.允许携带凭证与自定义请求头
     * <p>
     * @author ZuiM
     * @return CorsConfigurationSource 跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}