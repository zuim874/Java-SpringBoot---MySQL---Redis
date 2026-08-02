package com.xuwenye.demo.util.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // 引入密钥和过期时间配置
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    // Token 格式：头部.荷载.签名 Header(Base64URL编码).Payload(Base64URL编码).Signature(防篡改核心)
    // 头部：声明加密算法
    // 荷载：核心业务数据，存放用户信息，过期时间
    // 签名：检验 Token 是否被篡改
    // 生成 Token
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)      // 荷载 sub 字段：存入用户名
                .issuedAt(new Date())   // iat：签发时间（当前系统时间）
                .expiration(new Date(System.currentTimeMillis() + expirationMs))    // exp：过期时间
                .signWith(secretKey)    // 使用密钥做HS256签名，防止篡改
                .compact();             // 拼接成三段式JWT字符串返回
    }

    // 根据传入的密钥，解析出登录用户名（subject(username)）
    public String parseUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validate(String token) {
        try {
            // 1.调用parseClaims做三段基础校验
            Claims claims = parseClaims(token);
            // 2.判断exp过期时间是否在当前时间之前（是否过期）
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // 解析校验方法，统一封装签名，过期校验
    private Claims parseClaims(String token) {
        return Jwts.parser()    // 创建解析器构造器
                .verifyWith(secretKey)  // 绑定签名校验密钥
                .build()
                .parseSignedClaims(token)   // 核心校验，只允许带合法签名的JWT令牌
                .getPayload();
    }
}