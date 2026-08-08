package com.xuwenye.demo.dto.response;

/**
 * 登录响应 DTO
 * 1.token：JWT 令牌
 * 2.nickname：用户昵称
 * <p>
 * @author ZuiM
 */
public class LoginResponse {
    /** JWT 令牌 */
    private String token;
    /** 用户昵称 */
    private String nickname;

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
     */
    public LoginResponse(String token, String nickname) {
        this.token = token;
        this.nickname = nickname;
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
}
