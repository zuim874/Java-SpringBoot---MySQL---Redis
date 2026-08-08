package com.xuwenye.demo.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * SQL 注入安全拦截器：校验请求参数，防止恶意 SQL 注入
 * 1.遍历请求所有参数
 * 2.用预编译正则匹配危险字符
 * 3.命中则返回 400 并拦截，否则放行
 * <p>
 * @author ZuiM
 */
public class SqlInjectSafeInterceptor implements HandlerInterceptor {
    private static final String DANGER_REG = ".*('|--|#|<|>|select\\s+|drop\\s+|delete\\s+|insert\\s+|union\\s+|or\\s+1=1).*";

    /** 预编译正则（忽略大小写），避免每次请求重复编译 */
    private static final Pattern DANGER_PATTERN = Pattern.compile(DANGER_REG, Pattern.CASE_INSENSITIVE);

    /**
     * 请求前置拦截
     * 1.遍历请求参数
     * 2.匹配危险字符（单引号/--/#/关键字等）
     * 3.命中则写入 400 JSON 并拦截请求，否则放行
     * <p>
     * @author ZuiM
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 目标处理器
     * @return boolean true=放行，false=拦截
     * @throws Exception 响应写入异常
     */
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
