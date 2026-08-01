package com.xuweney.demo.dto.response;

/**
 * 登录响应 DTO
 */
public class LoginResponse {
    private String token;
    private String nickname;

    public LoginResponse() {}

    public LoginResponse(String token, String nickname) {
        this.token = token;
        this.nickname = nickname;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}