# JWT 认证实现指南：Token 验证与加密

> 本文档详细说明如何在当前项目中新增 JWT Token 认证机制。
> 按照步骤操作，即可完成从"无状态登录"到"完整的 Token 认证系统"的升级。

---

## 目录

- [一、当前项目状态分析](#一当前项目状态分析)
- [二、什么是 JWT？为什么要用 JWT？](#二什么是-jwt为什么要用-jwt)
- [三、实现步骤总览](#三实现步骤总览)
- [四、后端改造步骤](#四后端改造步骤)
  - [4.1 添加 JWT 依赖](#41-添加-jwt-依赖)
  - [4.2 配置 JWT 参数](#42-配置-jwt-参数)
  - [4.3 创建 JWT 工具类（精简版）](#43-创建-jwt-工具类精简版)
  - [4.4 创建 JWT 过滤器（精简版）](#44-创建-jwt-过滤器精简版)
  - [4.5 创建统一返回格式（Result）](#45-创建统一返回格式result)
  - [4.6 全局异常处理](#46-全局异常处理)
  - [4.7 修改 SecurityConfig 配置](#47-修改-securityconfig-配置)
  - [4.8 修改 AuthController 登录接口](#48-修改-authcontroller-登录接口)
  - [4.9 新增测试接口](#49-新增测试接口)
- [五、前端改造步骤](#五前端改造步骤)
  - [5.1 登录成功后保存 Token](#51-登录成功后保存-token)
  - [5.2 封装 HTTP 请求工具](#52-封装-http-请求工具)
  - [5.3 修改登录页面](#53-修改登录页面)
  - [5.4 退出登录时清除 Token](#54-退出登录时清除-token)
  - [5.5 添加路由守卫（可选）](#55-添加路由守卫可选)
- [六、完整测试流程](#六完整测试流程)
- [七、常见问题排查](#七常见问题排查)
  - [7.1 Token 过期前端不跳转登录页](#71-token-过期前端不跳转登录页)
- [八、进阶优化建议](#八进阶优化建议)

---

## 一、当前项目状态分析

### 1.1 已有的功能

✅ 用户登录接口（`/auth/login`）  
✅ BCrypt 密码加密  
✅ Spring Security 基础配置  
✅ CORS 跨域配置  

### 1.2 缺少的功能

❌ **JWT Token 生成**：登录成功后没有返回 Token  
❌ **Token 验证机制**：后端没有验证 Token 的过滤器  
❌ **前端 Token 管理**：前端没有保存和发送 Token 的逻辑  
❌ **无状态认证**：每次请求都需要重新验证身份，没有"记住登录状态"

### 1.3 改造目标

实现完整的 JWT 认证流程：

```
用户登录 → 后端验证 → 返回 JWT Token → 前端保存 Token
    ↓
后续请求 → 前端带上 Token → 后端验证 Token → 允许访问
```

---

## 二、什么是 JWT？为什么要用 JWT？

### 2.1 JWT 是什么？

JWT（JSON Web Token）是一种紧凑的、URL 安全的令牌格式，用于在各方之间安全地传输信息。

**JWT 的结构：**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQs5-wc
```

这个字符串由三部分组成，用 `.` 分隔：

1. **Header（头部）**：声明算法和类型
   ```json
   {
     "alg": "HS256",  // 使用 HS256 算法
     "typ": "JWT"     // 类型是 JWT
   }
   ```

2. **Payload（负载）**：存储实际的数据（claims）
   ```json
   {
     "sub": "admin",           // 主题（用户名）
     "iat": 1516239022,        // 签发时间
     "exp": 1516242622         // 过期时间（1小时后）
   }
   ```

3. **Signature（签名）**：防止数据被篡改
   ```
   HMACSHA256(
     base64UrlEncode(header) + "." + base64UrlEncode(payload),
     secretKey
   )
   ```

### 2.2 为什么要用 JWT？

| 对比项 | Session 方式 | JWT 方式 |
|--------|-------------|---------|
| 存储位置 | 服务端（内存/Redis） | 客户端（浏览器） |
| 扩展性 | 差（多台服务器需要共享 Session） | 好（无状态，每台服务器都能验证） |
| 跨域 | 麻烦（需要 Cookie 跨域） | 简单（放在 Header 中） |
| 移动端 | 不支持 Cookie | 完美支持 |
| 安全性 | 依赖 Session ID 保密 | 依赖签名和密钥保密 |

**JWT 的优势：**
1. **无状态**：服务端不需要存储 Session，节省内存
2. **可扩展**：适合分布式系统、微服务架构
3. **跨平台**：任何语言、任何设备都能使用
4. **自包含**：Token 中包含了用户信息，减少数据库查询

### 2.3 JWT 认证流程

```
┌─────────┐                         ┌─────────┐
│  前端    │                         │  后端    │
└────┬────┘                         └────┬────┘
     │                                   │
     │  1. POST /auth/login              │
     │  { username, password }           │
     │ ─────────────────────────────────>│
     │                                   │  2. 验证用户名密码
     │                                   │  3. 生成 JWT Token
     │  4. 返回 Token                    │
     │ <─────────────────────────────────│
     │                                   │
     │  5. 保存 Token 到 localStorage    │
     │                                   │
     │  6. GET /api/user/info            │
     │  Header: Authorization: Bearer xxx│
     │ ─────────────────────────────────>│
     │                                   │  7. 提取并验证 Token
     │                                   │  8. 解析出用户名
     │                                   │  9. 查询用户信息
     │  10. 返回用户数据                 │
     │ <─────────────────────────────────│
```

---

## 三、实现步骤总览

### 3.1 后端需要做的事

1. 添加 JWT 依赖（jjwt 库）
2. 创建 JWT 工具类（生成、验证、解析 Token）
3. 创建 JWT 过滤器（拦截请求，验证 Token）
4. 修改 SecurityConfig（注册过滤器，配置放行规则）
5. 修改 AuthController（登录成功后生成并返回 Token）
6. （可选）新增注册接口

### 3.2 前端需要做的事

1. 登录成功后保存 Token（localStorage）
2. 封装 HTTP 请求工具（自动带上 Token）
3. 修改登录页面（使用封装的工具）
4. 退出登录时清除 Token
5. （可选）添加路由守卫（未登录不能访问某些页面）

---

## 四、后端改造步骤

### 4.1 添加 JWT 依赖

**文件：** `pom.xml`

**位置：** 在 `<dependencies>` 标签内，添加以下依赖：

```xml
<!-- JWT 依赖 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**说明：**
- `jjwt-api`：JWT 的核心 API
- `jjwt-impl`：实现类
- `jjwt-jackson`：用于 JSON 序列化/反序列化

**操作：** 添加后，在 IDEA 中右键 `pom.xml` → Maven → Reload Project，让 Maven 下载依赖。

---

### 4.2 配置 JWT 参数

**文件：** `src/main/resources/application.properties`

**在文件末尾添加：**

```properties
# ========== JWT 配置 ==========
jwt.secret=YourSuperSecretKeyForJWTTokenGenerationMustBeAtLeast32CharactersLong
jwt.expiration-ms=86400000
```

**说明：**
- `jwt.secret`：JWT 签名密钥，至少 32 个字符，**生产环境务必改成复杂随机字符串**
- `jwt.expiration-ms`：Token 有效期，单位毫秒。86400000 = 24 小时

---

### 4.3 创建 JWT 工具类（精简版）

**新建文件：** `src/main/java/com/xuweney/demo/util/JwtUtil.java`

**完整代码：**

```java
package com.xuweney.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String parseUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validate(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

**代码解释：**

整个工具类只有 **3 个公开方法**，非常精简：

| 方法 | 作用 |
|------|------|
| `generateToken(username)` | 根据用户名生成 Token |
| `parseUsername(token)` | 从 Token 中解析出用户名 |
| `validate(token)` | 验证 Token 是否有效（签名正确 + 未过期） |

**为什么比之前的版本精简？**

1. **去掉了中间方法**：`extractClaim`、`extractExpiration`、`isTokenExpired` 这些方法只在内部用一次，直接内联更简洁
2. **构造函数注入配置**：密钥和过期时间从配置文件读取，不再硬编码
3. **方法命名更短**：`parseUsername` 比 `extractUsername` 更直观

---

### 4.4 创建 JWT 过滤器（精简版）

**新建文件：** `src/main/java/com/xuweney/demo/config/JwtAuthenticationFilter.java`

**完整代码：**

```java
package com.xuweney.demo.config;

import com.xuweney.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = null;

            try {
                username = jwtUtil.parseUsername(token);
            } catch (Exception ignored) {}

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtUtil.validate(token)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, null);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**为什么比之前的版本精简？**

1. **去掉了 logger**：对于简单项目，Token 解析失败的警告日志不是必须的
2. **减少了中间变量**：`username`、`jwtToken` 等变量在使用时才声明
3. **合并了 if 判断**：把多个 if 合并成一个，减少嵌套
4. **代码行数减半**：从 50 多行精简到 30 多行，逻辑一目了然

**过滤器做了什么？（4 句话讲清楚）**

1. 从请求头 `Authorization` 中取出 Token
2. 从 Token 解析出用户名
3. 如果 Token 有效，设置认证信息到 `SecurityContext`
4. 继续执行后续过滤器

---

### 4.5 创建统一返回格式（Result）

> 这一步虽然不是 JWT 必须的，但可以让代码更规范，前后端对接更舒服。

**新建文件：** `src/main/java/com/xuweney/demo/common/Result.java`

**完整代码：**

```java
package com.xuweney.demo.common;

public class Result<T> {
    private int code;
    private String mes;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.mes = "成功";
        r.data = data;
        return r;
    }

    public static Result<?> error(int code, String mes) {
        Result<?> r = new Result<>();
        r.code = code;
        r.mes = mes;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

**好处：**
- 所有接口返回格式统一
- 不用每次都 `new HashMap<>()`
- 语义更清晰（`Result.ok()` 一看就知道成功）

---

### 4.6 全局异常处理

> 让业务异常返回统一 JSON 格式。

**新建文件：** `src/main/java/com/xuweney/demo/common/GlobalExceptionHandler.java`

**完整代码：**

```java
package com.xuweney.demo.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        return Result.error(500, "服务器异常：" + e.getMessage());
    }
}
```

**说明：**
- `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
- 所有 Controller 抛出的异常都会被这里捕获
- 返回统一的 `Result` 格式

---

### 4.6.2 自定义认证失败处理器（解决 Token 过期不跳转问题）

> **为什么需要这个？**  
> Token 过期时，Spring Security 默认返回 HTML 错误页面（不是 JSON），前端无法判断。
> 所以我们要自定义一个处理器，认证失败时返回 JSON 格式的 401。

**新建文件：** `src/main/java/com/xuweney/demo/config/JwtAuthenticationEntryPoint.java`

**完整代码：**

```java
package com.xuweney.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuweney.demo.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Result<?> result = Result.error(401, "未登录或Token已过期，请重新登录");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

**说明：**
- `AuthenticationEntryPoint` 是 Spring Security 提供的接口
- 当用户未认证（没有 Token 或 Token 无效）时，会调用这个类的 `commence` 方法
- 我们在这里返回统一的 JSON 格式：`{ "code": 401, "mes": "未登录或Token已过期，请重新登录" }`

然后需要在 `SecurityConfig` 中注册这个处理器（下一步 4.7 会包含）。

---

### 4.7 修改 SecurityConfig 配置

**文件：** `src/main/java/com/xuweney/demo/config/SecurityConfig.java`

**修改后的完整代码：**

```java
package com.xuweney.demo.config;

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

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
```

**关键改动说明：**

| 改动 | 为什么 |
|------|--------|
| 构造函数注入 `jwtAuthenticationFilter` | 替代 `@Autowired` 字段注入，Spring 推荐做法 |
| `SessionCreationPolicy.STATELESS` | **重要**：JWT 是无状态的，告诉 Spring Security 不要创建 Session |
| 变量名简化（`cfg`、`auth`、`f` 等） | 代码更紧凑，不影响可读性 |

---

### 4.8 修改 AuthController 登录接口

**文件：** `src/main/java/com/xuweney/demo/Controller/AuthController.java`

**修改后的完整代码：**

```java
package com.xuweney.demo.Controller;

import com.xuweney.demo.Entity.User;
import com.xuweney.demo.Service.UserService;
import com.xuweney.demo.common.Result;
import com.xuweney.demo.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam String username,
                           @RequestParam String password) {
        User user = userService.findUsername(username);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(username);
        return Result.ok(Map.of(
            "token", token,
            "nickname", user.getNickname()
        ));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String nickname) {
        if (userService.findUsername(username) != null) {
            return Result.error(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setCreateTime(java.time.LocalDateTime.now());

        return userService.save(user)
            ? Result.ok("注册成功")
            : Result.error(500, "注册失败");
    }
}
```

**为什么更精简？**

1. **用 `Result<?>` 替代 `Map<String, Object>`**：类型更清晰，不用每次 new HashMap
2. **构造函数注入**：替代 `@Autowired`，更规范
3. **早返回（early return）**：不满足条件直接 return，减少嵌套
4. **去掉中间变量**：代码更紧凑

---

### 4.9 新增测试接口（验证 Token 是否生效）

**文件：** `src/main/java/com/xuweney/demo/Controller/TestController.java`

**修改后的代码：**

```java
package com.xuweney.demo.Controller;

import com.xuweney.demo.common.Result;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hello")
    public Result<String> hello(Authentication authentication) {
        String username = authentication.getName();
        return Result.ok("✅ 登录成功！当前用户：" + username);
    }
}
```

**验证方法：**

1. 先调用登录接口获取 Token
2. 在请求头中加上 `Authorization: Bearer <token>`
3. 访问 `GET /hello`，应该能看到当前用户名
4. 如果不带 Token，会返回 403 错误

---

## 五、前端改造步骤

### 5.1 登录成功后保存 Token

**文件：** `frontend/src/views/LoginView.vue`

**需要修改 `handleLogin()` 函数：**

找到以下代码（大约在第 47-53 行）：

```javascript
const data = await res.json()
if (data.code === 200) {
  success.value = true
  message.value = data.mes
  setTimeout(() => {
    router.push('/home')
  }, 800)
}
```

**修改为：**

```javascript
const data = await res.json()
if (data.code === 200) {
  success.value = true
  message.value = data.mes

  // 新增：保存 Token 到 localStorage
  localStorage.setItem('token', data.data.token)
  localStorage.setItem('nickname', data.data.nickname)

  setTimeout(() => {
    router.push('/home')
  }, 800)
}
```

**说明：**

- `localStorage.setItem('token', data.data.token)`：将 Token 保存到浏览器的本地存储中
- Token 会一直保存，直到用户手动清除或退出登录

---

### 5.2 封装 HTTP 请求工具

**新建文件：** `frontend/src/utils/request.js`

**完整代码：**

```javascript
// 封装 HTTP 请求工具，自动带上 Token，自动处理 401 跳转登录

const BASE_URL = '/api'  // 后端接口前缀（会被 Vite 代理）

// 清除登录信息并跳转到登录页
function clearAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  window.location.href = '/login'
}

/**
 * 发送 HTTP 请求
 *
 * @param {string} url - 接口路径（如 /auth/login）
 * @param {object} options - 请求配置
 * @returns {Promise} 响应数据
 */
export async function request(url, options = {}) {
  const token = localStorage.getItem('token')

  const defaultOptions = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  }

  if (token) {
    defaultOptions.headers['Authorization'] = `Bearer ${token}`
  }

  const mergedOptions = { ...defaultOptions, ...options }

  if (options.body instanceof URLSearchParams) {
    mergedOptions.headers['Content-Type'] = 'application/x-www-form-urlencoded'
  }

  try {
    const res = await fetch(BASE_URL + url, mergedOptions)

    // 关键：HTTP 状态码为 401 时，直接跳转登录页
    if (res.status === 401) {
      clearAndRedirect()
    }

    const data = await res.json()

    // 再兜底判断一下返回体里的 code（双重保险）
    if (data.code === 401) {
      clearAndRedirect()
    }

    return data
  } catch (error) {
    console.error('请求失败:', error)
    throw error
  }
}
```

**代码解释：**

1. **`clearAndRedirect()`**：把"清除 Token + 跳转登录页"抽成一个函数，避免重复代码。

2. **`if (res.status === 401)`**：**第一层判断**——HTTP 状态码为 401 时直接跳转。
   - 这是最关键的修复！之前只判断 `data.code`，但 Spring Security 返回 401 时 body 可能不是标准 JSON
   - HTTP 状态码是最可靠的判断依据

3. **`if (data.code === 401)`**：**第二层判断**——返回体里的 code 也是 401，再兜底判断一次。
   - 双重保险，确保任何情况下 401 都能正确跳转

4. **为什么会有这个问题？**

   ```
   旧逻辑的问题：
   Token 过期 → Spring Security 返回 403/401 → body 是 HTML 错误页
     → 前端 res.json() 解析失败 → 进入 catch → data.code 永远不会是 401
     
   新逻辑：
   Token 过期 → Spring Security 返回 401
     → res.status === 401 直接跳转 → 不需要等解析 JSON
   ```

---

### 5.3 每次请求自动带上 Token

**文件：** `frontend/src/views/LoginView.vue`

**需要修改的地方：**

1. 导入 `request` 工具
2. 使用 `request` 替代 `fetch`

**修改步骤：**

在 `<script setup>` 标签的开头（第 23-25 行附近），添加导入：

```javascript
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'  // 新增：导入请求工具
```

然后修改 `handleLogin()` 函数中的请求部分（第 38-46 行）：

**原代码：**

```javascript
const params = new URLSearchParams()
params.append('username', username.value)
params.append('password', password.value)

const res = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: params
})
const data = await res.json()
```

**修改为：**

```javascript
const params = new URLSearchParams()
params.append('username', username.value)
params.append('password', password.value)

const data = await request('/auth/login', {
  method: 'POST',
  body: params
})
```

**说明：**

- 使用 `request` 工具后，不需要手动设置 `Authorization` 头，`request` 会自动添加
- 也不需要写 `const data = await res.json()`，`request` 已经处理好了

---

### 5.4 退出登录时清除 Token

**文件：** `frontend/src/views/HomeView.vue`

**需要修改 `handleLogout()` 函数：**

找到以下代码（大约在第 10-12 行）：

```javascript
function handleLogout() {
  router.push('/login')
}
```

**修改为：**

```javascript
function handleLogout() {
  // 清除 Token 和用户信息
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')

  // 跳转到登录页
  router.push('/login')
}
```

**说明：**

- 退出登录时，必须清除 `localStorage` 中保存的 Token
- 否则即使跳转到登录页，Token 仍然有效，用户可以直接访问受保护的页面

---

### 5.5 添加路由守卫（可选）

**文件：** `frontend/src/router/index.js`

**作用：** 未登录用户不能访问 `/home` 等受保护的页面，自动跳转到登录页。

**修改后的完整代码：**

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/home', name: 'Home', component: HomeView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 新增：路由守卫
router.beforeEach((to, from, next) => {
  // 获取 Token
  const token = localStorage.getItem('token')

  // 如果要访问的页面需要登录（如 /home），但没有 Token
  if (to.name !== 'Login' && !token) {
    // 跳转到登录页
    next({ name: 'Login' })
  }
  // 如果已经登录，但要去登录页
  else if (to.name === 'Login' && token) {
    // 直接跳转到主页
    next({ name: 'Home' })
  }
  // 其他情况，正常跳转
  else {
    next()
  }
})

export default router
```

**代码解释：**

- **`router.beforeEach()`**：全局前置守卫，在每次路由跳转前执行
- **`to`**：要跳转到的目标路由
- **`from`**：当前正要离开的路由
- **`next()`**：执行跳转。可以传入参数指定跳转目标

**逻辑：**

1. 如果要访问的页面不是登录页，且没有 Token → 跳转到登录页
2. 如果已经登录（有 Token），但要去登录页 → 直接跳转到主页
3. 其他情况 → 正常跳转

---

## 六、完整测试流程

### 6.1 启动项目

1. **启动后端：** 在 IDEA 中运行 `DemoApplication.java`
2. **启动前端：** 在 `frontend` 目录下运行 `npm run dev`

### 6.2 测试登录

1. 打开浏览器，访问 `http://localhost:5173`
2. 输入用户名 `admin`，密码 `123456`
3. 点击"登录"按钮
4. 登录成功后，打开浏览器开发者工具（F12）→ Application → Local Storage
5. 检查是否保存了 `token` 和 `nickname`

### 6.3 测试 Token 验证

1. 登录成功后，访问 `http://localhost:5173/home`
2. 打开开发者工具 → Network 标签
3. 查看请求头，检查是否包含 `Authorization: Bearer <token>`

### 6.4 测试退出登录

1. 在主页点击"退出登录"按钮
2. 检查 Local Storage 中的 `token` 是否被清除
3. 尝试直接访问 `http://localhost:5173/home`，应该被跳转到登录页

### 6.5 测试 Token 过期

1. 修改 `JwtUtil.java` 中的 `EXPIRATION_TIME` 为 10 秒（方便测试）
   ```java
   private static final long EXPIRATION_TIME = 10 * 1000;  // 10秒
   ```
2. 重新启动后端
3. 登录后等待 10 秒
4. 刷新页面或访问其他接口，应该被跳转到登录页

---

## 七、常见问题排查

### 7.1 Token 过期前端不跳转登录页

**问题现象：**
把 Token 有效期设为 10 秒，过期后访问接口，前端没有跳转到登录页。

**根本原因：**

```
Token 过期 → JWT 过滤器不设置认证信息 → Spring Security 拦截请求
→ 默认返回 HTML 错误页面（不是 JSON）
→ 前端 res.json() 解析失败 → 进入 catch
→ data.code === 401 判断永远不会触发
→ 不跳转
```

**解决方案（前后端各改一处）：**

**后端**：新增 `JwtAuthenticationEntryPoint`，让认证失败时返回 JSON 格式的 401（见 4.6.2 节）

**前端**：先判断 `res.status === 401`（HTTP 状态码），再判断 `data.code === 401`（返回体 code），双重保险（见 5.2 节）

**验证方法：**
1. 把 `jwt.expiration-ms` 改成 `10000`（10秒）
2. 登录后访问 `/hello`，应该能看到用户名
3. 等 10 秒后刷新页面或再次访问
4. 应该自动跳转到 `/login` 页面

---

### 7.2 前端报错"Invalid Token"

**可能原因：**
- Token 格式不正确（没有 `Bearer ` 前缀）
- Token 已过期
- 密钥不一致（生成和验证用的密钥不同）

**解决方法：**
- 检查请求头格式：`Authorization: Bearer <token>`
- 检查 Token 是否过期（查看 `exp` 字段）
- 确保前后端使用相同的密钥

### 7.2 后端返回 401 Unauthorized

**可能原因：**
- 请求头中没有 `Authorization` 字段
- Token 无效或过期
- `JwtAuthenticationFilter` 没有正确注册

**解决方法：**
- 检查前端是否正确发送了 Token
- 在 `JwtAuthenticationFilter` 中打印日志，查看是否进入过滤器
- 检查 `SecurityConfig` 中是否添加了 `.addFilterBefore(jwtAuthenticationFilter, ...)`

### 7.3 跨域错误

**可能原因：**
- CORS 配置不允许 `Authorization` 头

**解决方法：**
- 检查 `SecurityConfig` 中的 CORS 配置，确保 `allowedHeaders` 包含 `*` 或 `Authorization`

### 7.4 Token 保存失败

**可能原因：**
- 浏览器禁用了 localStorage
- 使用了隐私模式

**解决方法：**
- 检查浏览器设置
- 使用普通模式打开浏览器

---

## 八、进阶优化建议

### 8.1 密钥管理

**问题：** 当前密钥硬编码在代码中，不安全。

**解决方案：** 从配置文件读取密钥。

**步骤：**

1. 在 `application.properties` 中添加：
   ```properties
   jwt.secret=YourSuperSecretKeyForJWTTokenGenerationMustBeAtLeast32CharactersLong
   jwt.expiration=86400000
   ```

2. 修改 `JwtUtil.java`：
   ```java
   @Value("${jwt.secret}")
   private String secretKey;

   @Value("${jwt.expiration}")
   private long expirationTime;
   ```

### 8.2 Token 刷新机制

**问题：** Token 过期后，用户需要重新登录，体验不好。

**解决方案：** 实现双 Token 机制（Access Token + Refresh Token）。

- **Access Token**：短期有效（如 2 小时），用于访问接口
- **Refresh Token**：长期有效（如 7 天），用于刷新 Access Token

当 Access Token 过期时，前端用 Refresh Token 请求新的 Access Token，无需重新登录。

### 8.3 Token 黑名单

**问题：** 用户退出登录后，Token 仍然有效（直到过期）。

**解决方案：** 使用 Redis 存储已退出的 Token。

**步骤：**

1. 用户退出登录时，将 Token 存入 Redis，设置过期时间与 Token 剩余有效期相同
2. 每次请求时，`JwtAuthenticationFilter` 检查 Token 是否在黑名单中
3. 如果在黑名单中，拒绝访问

### 8.4 统一返回格式

**问题：** 当前返回格式不统一（有时用 `HashMap`，有时用 `Map.of`）。

**解决方案：** 创建统一的 `Result` 类。

```java
public class Result<T> {
    private int code;
    private String mes;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.mes = "成功";
        r.data = data;
        return r;
    }

    public static Result<?> error(int code, String mes) {
        Result<?> r = new Result<>();
        r.code = code;
        r.mes = mes;
        return r;
    }

    // getter 和 setter ...
}
```

### 8.5 全局异常处理

**问题：** 如果 Token 解析出错，会返回 500 错误，不友好。

**解决方案：** 添加全局异常处理器。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}
```

---

## 总结

完成以上步骤后，你的项目将具备完整的 JWT 认证功能：

✅ 登录成功后返回 Token  
✅ 前端保存 Token 到 localStorage  
✅ 每次请求自动带上 Token  
✅ 后端验证 Token 有效性  
✅ Token 过期自动跳转登录页  
✅ 退出登录清除 Token  

这是一个企业级项目中常用的认证方案，也是面试中经常问到的知识点。

**下一步建议：**
- 实现注册功能
- 添加用户管理功能（查看、编辑、删除用户）
- 实现 Token 刷新机制
- 使用 Redis 存储 Token 黑名单

祝你学习顺利！
