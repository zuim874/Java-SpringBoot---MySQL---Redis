package com.xuwenye.demo.dto.response;

import java.util.Arrays;
import java.util.List;

/**
 * 登录响应 DTO
 * 1.token：JWT 令牌
 * 2.nickname：用户昵称
 * 3.roles：用户角色列表（前端用于管理员/卖家入口与路由鉴权）
 * <p>
 * @author ZuiM
 */
public class LoginResponse {
    /** JWT 令牌 */
    private String token;
    /** 用户昵称 */
    private String nickname;
    /** 用户角色列表（如 ROLE_ADMIN/ROLE_SELLER/ROLE_USER） */
    private List<String> roles;

    /**
     * 无参构造（反序列化用）
     * <p>
     * @author ZuiM
     */
    public LoginResponse() {}

    /**
     * 全参构造
     * <p>
     * @author ZuiM
     * @param token JWT 令牌
     * @param nickname 用户昵称
     * @param userRole 用户角色（单字段，包装为列表返回）
     */
    public LoginResponse(String token, String nickname, String userRole) {
        this.token = token;
        this.nickname = nickname;
        this.roles = (userRole == null || userRole.isBlank())
                ? Arrays.asList("ROLE_USER")
                : Arrays.asList(userRole);
    }

    /**
     * 获取 JWT 令牌
     * <p>
     * @author ZuiM
     * @return String JWT 令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置 JWT 令牌
     * <p>
     * @author ZuiM
     * @param token JWT 令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取用户昵称
     * <p>
     * @author ZuiM
     * @return String 用户昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置用户昵称
     * <p>
     * @author ZuiM
     * @param nickname 用户昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 获取用户角色列表
     * <p>
     * @author ZuiM
     * @return List<String> 角色列表
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * 设置用户角色列表
     * <p>
     * @author ZuiM
     * @param roles 角色列表
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
