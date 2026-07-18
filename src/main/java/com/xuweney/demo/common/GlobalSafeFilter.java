package com.xuweney.demo.common;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class GlobalSafeFilter implements Filter {
    private static final String DANGER_CHAR = "'|--|#|<|>|select\\s|drop\\s|delete\\s|insert\\s|union\\s|or\\s+1=1";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");

        for (String paramKey : req.getParameterMap().keySet()) {
            String paramVal = req.getParameter(paramKey);
            if (paramVal != null && paramVal.toLowerCase().matches(".*(" + DANGER_CHAR + ").*")) {
                resp.setContentType("application/json;charset=utf-8");
                resp.getWriter().write("{\"code\":400,\"mes\":\"参数包含非法字符，访问被拦截\",\"data\":null}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}