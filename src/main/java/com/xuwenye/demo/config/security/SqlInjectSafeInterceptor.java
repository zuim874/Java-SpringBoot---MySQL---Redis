package com.xuwenye.demo.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * SQL 注入安全拦截器：校验请求参数，防止恶意 SQL 注入
 * 1.遍历请求所有参数
 * 2.用预编译正则匹配完整的注入语句特征（而非单个字符）
 * 3.命中则返回 400 并拦截，否则放行
 * <p>
 * 设计原则：项目使用 MyBatis 预编译（#{}），参数值中的单个字符
 * （如单引号 '、#、<、>）本身不会造成注入，不能一刀切拦截，
 * 否则会误伤正常密码（如包含特殊字符的强密码）。
 * 只有命中完整的 SQL 语句特征（注释符、关键字序列、典型绕过表达式）
 * 才判定为注入攻击。
 * <p>
 * @author ZuiM
 */
@Slf4j
public class SqlInjectSafeInterceptor implements HandlerInterceptor {

    /**
     * 注入特征正则（忽略大小写）：
     * 1.--            SQL 行注释
     * 2.SQL 块注释
     * 3.union select  联合查询注入
     * 4.select..from  查询注入
     * 5.drop/delete/insert/alter/update 破坏性语句
     * 6.' or ' / ' and ' 单引号闭合绕过
     * 7.or 1=1         恒真条件绕过
     * 8.exec(          存储过程执行
     */
    private static final String DANGER_REG = ".*(--|/\\*|\\*/|union\\s+select|select\\s+.+\\s+from|drop\\s+table|delete\\s+from|insert\\s+into|alter\\s+table|update\\s+\\w+\\s+set|'\\s*(or|and)\\s*'|or\\s+1\\s*=\\s*1|exec\\s*\\().*";

    /** 预编译正则（忽略大小写），避免每次请求重复编译 */
    private static final Pattern DANGER_PATTERN = Pattern.compile(DANGER_REG, Pattern.CASE_INSENSITIVE);

    /**
     * 请求前置拦截
     * 1.遍历请求参数
     * 2.匹配注入语句特征（注释符/关键字序列/典型绕过表达式）
     * 3.命中则写入 400 JSON 并拦截请求（同时输出 WARN 日志便于追溯），否则放行
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
                // 输出日志：记录被拦截的接口、参数名和参数值，便于排查误拦截
                log.warn("SQL 注入拦截：uri={}, 参数名={}, 参数值={}", request.getRequestURI(), paramName, val);
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":400,\"mes\":\"参数包含非法字符，访问被拦截\",\"data\":null}");
                return false;
            }
        }
        return true;
    }
}
