package com.xuwenye.demo.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * SQL 注入安全拦截器：校验请求参数，防止恶意 SQL 注入
 */
public class SqlInjectSafeInterceptor implements HandlerInterceptor {
    private static final String DANGER_REG = ".*('|--|#|<|>|select\\s+|drop\\s+|delete\\s+|insert\\s+|union\\s+|or\\s+1=1).*";

    /** 预编译正则（忽略大小写），避免每次请求重复编译 */
    private static final Pattern DANGER_PATTERN = Pattern.compile(DANGER_REG, Pattern.CASE_INSENSITIVE);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        for (String paramName : request.getParameterMap().keySet()) {
            String val = request.getParameter(paramName);
            if (val != null && DANGER_PATTERN.matcher(val).matches()) {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":400,\"mes\":\"参数包含非法字符，访问被拦截\",\"data\":null}");
                return false;
            }
        }
        return true;
    }
}
