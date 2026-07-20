# 项目学习文档：从零理解一个完整的 Web 应用

> 本文档面向完全零基础读者，由浅入深地讲解本项目涉及的所有知识。
> 即使你从未写过一行代码，也能跟着理解。

---

## 目录

- [第一章：先搞懂几个基本概念](#第一章先搞懂几个基本概念)
- [第二章：这个项目是做什么的？](#第二章这个项目是做什么的)
- [第三章：项目整体架构——前后端分离](#第三章项目整体架构前后端分离)
- [第四章：后端技术栈详解](#第四章后端技术栈详解)
- [第五章：后端代码逐行解读](#第五章后端代码逐行解读)
- [第六章：前端技术栈详解](#第六章前端技术栈详解)
- [第七章：前端代码逐行解读](#第七章前端代码逐行解读)
- [第八章：前后端怎么配合工作的？](#第八章前后端怎么配合工作的)
- [第九章：为什么这么写？有什么好处？](#第九章为什么这么写有什么好处)
- [第十章：有没有其他方案？](#第十章有没有其他方案)
- [第十一章：项目还缺少什么？](#第十一章项目还缺少什么)
- [第十二章：实习需要知道的额外知识](#第十二章实习需要知道的额外知识)

---

## 第一章：先搞懂几个基本概念

### 1.1 什么是"网站"？

你每天用的微信、淘宝、B站，本质上都是"网站"（或叫 Web 应用）。

一个网站由两部分组成：

| 部分 | 类比 | 做什么 |
|------|------|--------|
| **前端**（Frontend） | 餐厅的大堂 | 用户看到的页面、按钮、输入框 |
| **后端**（Backend） | 餐厅的后厨 | 处理业务逻辑、存取数据 |

除此之外，还需要：
- **数据库**（Database）：相当于餐厅的仓库，用来存放所有数据（用户信息、订单等）
- **缓存**（Cache）：相当于餐厅前台的货架，把常用的东西放在手边，拿得更快

### 1.2 什么是"编程语言"？

就像人类用中文、英文交流，计算机也需要"语言"来告诉它做什么：

- **Java**：后端用的语言（本项目），全球最流行的企业级语言之一
- **JavaScript**：前端用的语言，所有浏览器都支持
- **HTML**：不是编程语言，是"网页的骨架"，定义页面上有什么内容
- **CSS**：不是编程语言，是"网页的衣服"，定义页面长什么样
- **SQL**：跟数据库说话的语言，用来查数据、存数据

### 1.3 什么是"框架"？

框架就像"半成品模板"。

比如你要盖房子，框架就是已经打好的地基和承重墙，你只需要装修内部就行。

- **Spring Boot**（后端框架）：帮你搭好了 Java Web 应用的骨架，你不用从零配置
- **Vue**（前端框架）：帮你搭好了前端页面的骨架，你只需要写具体功能

### 1.4 什么是"接口"（API）？

前端和后端之间通过"接口"通信。

接口就像餐厅的菜单——前端说"我要一份宫保鸡丁"（发送请求），后端做好端回来（返回数据）。

接口有格式：
- **URL**：地址，比如 `/auth/login` 表示"登录接口"
- **方法**：动作，常见的有 `GET`（获取数据）和 `POST`（提交数据）
- **请求参数**：你提交的信息，比如用户名和密码
- **返回结果**：后端给你的回复，比如"登录成功"或"密码错误"

---

## 第二章：这个项目是做什么的？

### 2.1 一句话总结

**这是一个带登录功能的 Web 应用。** 用户可以输入用户名和密码登录，登录成功后进入主页。

### 2.2 功能清单

| 功能 | 描述 |
|------|------|
| 用户登录 | 输入用户名和密码，验证通过后跳转到主页 |
| 自动初始化管理员 | 项目第一次启动时，自动创建一个 admin 账号 |
| 密码加密存储 | 密码不是明文保存，而是加密后存储（更安全） |
| 权限控制 | 没登录的人只能访问登录接口，其他接口都需要登录 |

### 2.3 登录流程（用生活场景理解）

```
你（前端）  →  走到银行门口（打开网页）
            →  输入卡号和密码（填写登录表单）
            →  银行柜员验证你的身份（后端校验）
            →  验证通过，让你进入大厅（跳转到主页）
            →  验证失败，告诉你密码错误（显示错误提示）
```

---

## 第三章：项目整体架构——前后端分离

### 3.1 什么是"前后端分离"？

**传统方式**：前端和后端写在一起，后端直接生成 HTML 页面发给浏览器。

```
传统方式：
浏览器 → 后端（既处理逻辑，又生成页面）→ 返回完整 HTML 页面
```

**前后端分离**（本项目采用）：前端和后端是两个独立的项目，各自独立开发、独立运行。

```
前后端分离：
浏览器 → 前端（Vue，运行在 localhost:5173）
              ↓ 发送 HTTP 请求（要数据）
         后端（Spring Boot，运行在 localhost:8080）
              ↓ 查询数据库，处理逻辑
              ↓ 返回 JSON 数据（不是页面！）
前端 ← 收到 JSON 数据，自己渲染页面
```

### 3.2 为什么要前后端分离？

| 优点 | 说明 |
|------|------|
| 分工明确 | 前端工程师只管页面，后端工程师只管接口，互不干扰 |
| 独立部署 | 前端和后端可以分别更新，不用一起发布 |
| 多端复用 | 同一套后端接口，可以同时给网页、手机 App、小程序用 |

### 3.3 项目的文件结构

```
Java SpringBoot + MySQL + Redis/
├── src/                          ← 后端代码
│   └── main/
│       ├── java/com/xuweney/demo/
│       │   ├── DemoApplication.java        ← 启动入口（程序的"大门"）
│       │   ├── Controller/                 ← 控制器层（接收请求）
│       │   │   ├── AuthController.java     ←   登录相关接口
│       │   │   └── TestController.java     ←   测试接口
│       │   ├── Service/                    ← 服务层（处理业务逻辑）
│       │   │   └── UserService.java
│       │   ├── Mapper/                     ← 数据访问层（操作数据库）
│       │   │   └── UserMapper.java
│       │   ├── Entity/                     ← 实体类（对应数据库表）
│       │   │   └── User.java
│       │   └── config/                     ← 配置类
│       │       ├── SecurityConfig.java     ←   安全配置
│       │       ├── RedisConfig.java        ←   Redis 配置
│       │       └── InitAdminRunner.java    ←   初始化管理员
│       └── resources/
│           └── application.properties      ← 配置文件（数据库密码等）
├── frontend/                     ← 前端代码
│   ├── src/
│   │   ├── App.vue               ← 根组件
│   │   ├── main.js               ← 前端入口
│   │   ├── style.css             ← 全局样式
│   │   ├── router/index.js       ← 路由配置
│   │   └── views/                ← 页面
│   │       ├── LoginView.vue     ←   登录页
│   │       └── HomeView.vue      ←   主页
│   ├── package.json              ← 前端依赖配置
│   └── vite.config.js            ← Vite 构建工具配置
└── pom.xml                       ← 后端依赖配置（Maven）
```

---

## 第四章：后端技术栈详解

### 4.1 Java 20

Java 是本项目后端使用的编程语言。Java 20 是比较新的版本。

**为什么选 Java？**
- 全球最多的企业在使用，生态成熟
- 强类型语言，适合大型项目
- 招聘市场需求最大之一

### 4.2 Spring Boot 3.2.4

Spring Boot 是基于 Spring 框架的"快速启动器"。

**没有 Spring Boot 时**：你需要手动配置几十个文件，写上千行配置代码，才能让一个 Web 应用跑起来。

**有了 Spring Boot 后**：只需要一个注解 `@SpringBootApplication`，它就帮你自动配好了一切。

**核心思想：自动配置（Auto Configuration）**
> Spring Boot 会根据你引入了哪些依赖，自动帮你配置好对应的功能。
> 比如你引入了 MySQL 驱动，它就自动帮你配好数据库连接。

### 4.3 Spring Security

负责"安全"的框架——谁能访问什么功能。

本项目用它实现：
- 密码加密（BCrypt 算法）
- 接口权限控制（没登录不能访问）
- 跨域配置（允许前端访问后端）

### 4.4 MyBatis-Plus

操作数据库的工具。

**原始方式**：你需要自己写 SQL 语句（`SELECT * FROM user WHERE username='admin'`），然后手动把查询结果映射到 Java 对象。

**MyBatis-Plus**：帮你自动生成常用的 SQL，你只需要调用方法就行：

```java
// 不用 MyBatis-Plus 时（繁琐）：
String sql = "SELECT * FROM sys_user WHERE username = ?";
// ... 几十行代码 ...

// 用了 MyBatis-Plus 后（简洁）：
wrapper.eq("username", username);
userMapper.selectOne(wrapper);   // 就这两行！
```

### 4.5 MySQL

关系型数据库，用来持久化存储数据。

"关系型"的意思是：数据存在"表"里，就像 Excel 表格一样，有行有列。

本项目的用户表：

| id | username | password | nickname | status | create_time |
|----|----------|----------|----------|--------|-------------|
| 1 | admin | $2a$10$xxx... | 超级管理员 | 1 | 2026-07-15 |

### 4.6 Redis

缓存数据库，数据存在内存里，读写速度比 MySQL 快得多。

**为什么需要 Redis？**
> 假设你有一个热门商品详情页，每天有 100 万人访问。
> 如果每次都去 MySQL 查，数据库会扛不住。
> 用 Redis 把数据缓存起来，第一次查 MySQL，之后 99 万次直接从 Redis 拿。

本项目引入了 Redis 依赖，但目前还没有实际使用缓存功能（这是可以改进的地方）。

### 4.7 Maven 和 pom.xml

Maven 是 Java 世界的"包管理器"，帮你下载和管理第三方库。

`pom.xml` 就是 Maven 的配置文件，类似于前端的 `package.json`。

```xml
<!-- 这行代码的意思是：我要引入 MySQL 的驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
</dependency>
```

你写了这行之后，Maven 会自动从网上下载这个库，你不需要手动去找。

### 4.8 Lombok

一个"偷懒"工具。

Java 的实体类通常需要写大量的 getter、setter、toString 方法，非常枯燥：

```java
// 不用 Lombok：需要手写这些
public class User {
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    // ... 每个字段都要写两套方法，10 个字段就是 20 个方法！
}

// 用了 Lombok：加一个注解就行
@Data   // 自动生成所有 getter、setter、toString、equals、hashCode
public class User {
    private String username;
}
```

---

## 第五章：后端代码逐行解读

### 5.1 启动入口：DemoApplication.java

```java
package com.xuweney.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication    // ① 这个注解表示"我是一个 Spring Boot 应用"
public class DemoApplication {
    public static void main(String[] args){
        SpringApplication.run(DemoApplication.class, args);  // ② 启动！
    }
}
```

**逐行解释：**

- **① `@SpringBootApplication`**：这是一个"注解"（Annotation），是 Java 的一种元数据标记。加上这个注解，Spring Boot 就知道："这个类是启动入口，请帮我自动配置所有需要的东西"。
  - 它其实是一个"组合注解"，内部包含了三个注解：
    - `@Configuration`：标记为配置类
    - `@EnableAutoConfiguration`：开启自动配置
    - `@ComponentScan`：自动扫描当前包及子包下的所有组件

- **② `SpringApplication.run()`**：这一行代码会启动一个内嵌的 Tomcat 服务器（默认监听 8080 端口），加载所有配置，创建所有 Bean（对象），最终让应用跑起来。

**为什么这么写？**
> Spring Boot 的设计理念是"约定大于配置"——你只需要加一个注解，剩下的它帮你搞定。
> 在 Spring Boot 出现之前，启动一个 Web 应用需要写几百行 XML 配置。

### 5.2 实体类：User.java

```java
@Data                           // Lombok：自动生成 getter/setter 等
@TableName("sys_user")         // MyBatis-Plus：这个类对应数据库的 sys_user 表
public class User {
    @TableId(type = IdType.AUTO)  // 主键，自增长
    private Long id;
    private String username;       // 用户名
    private String password;       // 密码（加密后的）
    private String nickname;       // 昵称
    private Integer status;        // 状态：0=禁用，1=启用
    private LocalDateTime createTime;  // 创建时间
}
```

**关键概念解释：**

- **实体类（Entity）**：Java 中的一个类，用来对应数据库中的一张表。类的每个字段对应表的一列。
- **`@TableName("sys_user")`**：告诉 MyBatis-Plus，这个类对应的是 `sys_user` 这张表。如果不写，默认用类名作为表名。
- **`@TableId(type = IdType.AUTO)`**：标记这是主键字段，`AUTO` 表示由数据库自动生成（自增长）。
- **`@Data`**：Lombok 提供的注解，自动帮你生成 `getXxx()`、`setXxx()`、`toString()` 等方法。

**为什么用 Long 而不是 int？**
> `Long` 是 64 位整数，范围比 `int`（32 位）大得多。
> 在实际项目中，数据量可能超过 21 亿（int 的上限），所以用 Long 更安全。
> 这也是阿里巴巴 Java 开发规范的要求。

### 5.3 数据访问层：UserMapper.java

```java
@Mapper   // 告诉 MyBatis：这是一个 Mapper 接口
public interface UserMapper extends BaseMapper<User> {
    // 什么都不用写！BaseMapper 已经提供了常用的数据库操作方法
}
```

**关键概念解释：**

- **Mapper（映射器）**：负责跟数据库打交道的层。你调用它的方法，它帮你执行 SQL。
- **`BaseMapper<User>`**：MyBatis-Plus 提供的基类，内置了十几个常用方法：

| 方法 | 作用 | 对应的 SQL |
|------|------|-----------|
| `selectOne(wrapper)` | 查询一条记录 | `SELECT * FROM sys_user WHERE ...` |
| `selectList(wrapper)` | 查询多条记录 | `SELECT * FROM sys_user WHERE ...` |
| `insert(entity)` | 插入一条记录 | `INSERT INTO sys_user ...` |
| `updateById(entity)` | 按 ID 更新 | `UPDATE sys_user SET ... WHERE id=?` |
| `deleteById(id)` | 按 ID 删除 | `DELETE FROM sys_user WHERE id=?` |

**为什么继承 BaseMapper 就行了？**
> 这就是"泛型"（Generic）的威力。`BaseMapper<User>` 告诉 Java：
> "我要一个专门操作 User 的 Mapper"，Java 会自动把所有方法的参数和返回值都适配成 User 类型。
> 这避免了为每张表都写一套重复的 CRUD 代码。

### 5.4 服务层：UserService.java

```java
@Service   // 标记为服务层组件，Spring 会自动创建它的实例
public class UserService {
    @Autowired   // 自动注入：Spring 帮你找到 UserMapper 的实例并赋值给这个变量
    private UserMapper userMapper;

    // 根据用户名查询用户
    public User findUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();  // 创建条件构造器
        wrapper.eq("username", username);  // 添加条件：username = 传入的参数
        return userMapper.selectOne(wrapper);  // 执行查询，返回一条记录
    }

    // 保存用户
    public boolean save(User user) {
        return userMapper.insert(user) > 0;  // 插入成功返回 true
    }
}
```

**关键概念解释：**

- **`@Service`**：标记这个类是"服务层"。Spring 启动时会创建这个类的对象（叫 Bean），并管理它的生命周期。
- **`@Autowired`**：自动注入。你不需要手动 `new UserMapper()`，Spring 会帮你找到已经创建好的 UserMapper 实例，自动赋值。这就是"依赖注入"（Dependency Injection，DI）。
- **`QueryWrapper`**：MyBatis-Plus 的条件构造器，用来构建查询条件。`wrapper.eq("username", username)` 等价于 SQL 中的 `WHERE username = 'xxx'`。

**为什么要分 Service 层？直接在 Controller 里查数据库不行吗？**
> 可以，但不推荐。分层的好处：
> 1. **职责单一**：Controller 只管接收请求和返回结果，Service 只管业务逻辑，Mapper 只管数据库
> 2. **可复用**：如果以后有另一个接口也需要"根据用户名查用户"，直接调用 Service 就行
> 3. **易于测试**：可以单独测试 Service 的逻辑，不需要启动 Web 服务器
> 4. **易于维护**：修改数据库操作逻辑时，只需要改 Service 和 Mapper，不用动 Controller

### 5.5 控制器层：AuthController.java

```java
@RestController      // ① 标记为 REST 控制器，返回值直接作为 HTTP 响应体
@RequestMapping("/auth")  // ② 这个控制器下所有接口的路径前缀是 /auth
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")   // ③ 处理 POST /auth/login 请求
    public Map<String, Object> login(
            @RequestParam String username,  // ④ 从请求中获取 username 参数
            @RequestParam String password   // 从请求中获取 password 参数
    ) {
        Map<String, Object> result = new HashMap<>();

        // 第一步：查询用户是否存在
        User user = userService.findUsername(username);
        if (user == null) {
            result.put("code", 401);
            result.put("mes", "用户不存在");
            return result;
        }

        // 第二步：校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("code", 401);
            result.put("mes", "密码错误");
            return result;
        }

        // 第三步：登录成功
        result.put("code", 200);
        result.put("mes", "登陆成功");
        result.put("data", Map.of("nickname", user.getNickname()));
        return result;
    }
}
```

**逐行解释：**

- **① `@RestController`**：等于 `@Controller` + `@ResponseBody`。意思是"我的返回值直接作为 HTTP 响应的内容（而不是跳转到某个页面）"。通常返回 JSON 格式。

- **② `@RequestMapping("/auth")`**：定义这个控制器的所有接口的公共前缀。下面的 `/login` 接口完整路径就是 `/auth/login`。

- **③ `@PostMapping("/login")`**：表示这个接口只接受 POST 请求，路径是 `/login`（加上类级别的前缀，完整路径是 `/auth/login`）。
  - 为什么用 POST 而不是 GET？因为登录涉及密码，GET 请求的参数会显示在 URL 中（不安全），POST 把参数放在请求体中。

- **④ `@RequestParam`**：从请求参数中获取值。前端发送 `username=admin&password=123456`，这个注解会自动把值提取出来。

**返回的 JSON 格式：**
```json
{
    "code": 200,
    "mes": "登陆成功",
    "data": {
        "nickname": "超级管理员"
    }
}
```

### 5.6 安全配置：SecurityConfig.java

```java
@Configuration        // 标记为配置类
@EnableWebSecurity    // 启用 Spring Security
public class SecurityConfig {

    // ① 密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ② 安全过滤链
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()  // 登录接口放行
                .anyRequest().authenticated()                // 其他都要认证
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    // ③ 跨域配置
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // ...
    }
}
```

**关键概念解释：**

- **① BCryptPasswordEncoder**：
  - BCrypt 是一种密码哈希算法。它把密码变成一个不可逆的"摘要"。
  - 比如 `123456` → `$2a$10$N9qo8uLOickgx2ZMRZoMye...`
  - **不可逆**：从摘要无法反推出原始密码。即使数据库被黑客偷了，他也拿不到你的真实密码。
  - **每次加密结果不同**：BCrypt 会自动加"盐"（salt），所以同一个密码每次加密的结果不一样，但验证时能正确匹配。

- **② SecurityFilterChain**：
  - Spring Security 的核心配置。它定义了一条"过滤链"——每个请求进来都要经过这些过滤器。
  - `permitAll()`：允许所有人访问（不需要登录）
  - `authenticated()`：必须认证（登录）才能访问
  - `csrf.disable()`：关闭 CSRF 防护。前后端分离项目通常关闭它，因为 CSRF 防护是针对传统 Web 应用的。
  - `formLogin.disable()`：关闭 Spring Security 自带的登录页面（因为我们有自己的前端登录页）

- **③ CORS（跨域资源共享）**：
  - 浏览器的"同源策略"规定：前端（localhost:5173）不能直接请求后端（localhost:8080），因为端口不同，算"跨域"。
  - CORS 配置就是告诉浏览器："我允许 localhost:5173 来访问我"。
  - 如果不配置 CORS，前端发请求时会报"跨域错误"。

### 5.7 Redis 配置：RedisConfig.java

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 设置序列化方式：用字符串格式存储（方便在 Redis 客户端里查看）
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

**关键概念解释：**

- **RedisTemplate**：Spring Data Redis 提供的工具类，用来操作 Redis。就像 MyBatis-Plus 的 Mapper 用来操作 MySQL 一样。
- **序列化（Serializer）**：数据存入 Redis 时需要从 Java 对象转换成字节流（序列化），取出时再从字节流还原成 Java 对象（反序列化）。
  - `StringRedisSerializer`：用字符串格式存储。好处是在 Redis 命令行客户端里能直接看到内容，不会出现乱码。
  - 如果不设置，默认用 JDK 序列化，存进去的内容人类看不懂。

### 5.8 初始化管理员：InitAdminRunner.java

```java
@Component   // 标记为 Spring 组件，会被自动扫描到
public class InitAdminRunner implements CommandLineRunner {
    // CommandLineRunner：Spring Boot 启动完成后会自动执行它的 run 方法

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 如果 admin 用户不存在，就创建一个
        if (userService.findUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));  // 加密密码
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            admin.setCreateTime(LocalDateTime.now());
            userService.save(admin);
        }
    }
}
```

**关键概念解释：**

- **`CommandLineRunner`**：Spring Boot 提供的接口。实现了这个接口的类，在项目启动完成后会自动执行 `run()` 方法。
  - 常见用途：初始化数据、预热缓存、检查系统环境等。
- **`passwordEncoder.encode("123456")`**：把明文密码 `123456` 加密后再存入数据库。这样即使数据库泄露，攻击者也看不到原始密码。

### 5.9 配置文件：application.properties

```properties
# 应用名称
spring.application.name=demoApplication

# 服务器端口
server.port=8080

# MySQL 数据库连接配置
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/springbootdb?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456

# MyBatis-Plus 配置
mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml
mybatis-plus.global.config.db-config.id-type=auto

# Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

**逐行解释：**

- `server.port=8080`：后端服务运行在 8080 端口
- `spring.datasource.url`：数据库连接地址。`localhost:3306` 是 MySQL 的默认端口，`springbootdb` 是数据库名
- `spring.datasource.username/password`：数据库的用户名和密码
- `mybatis-plus.mapper-locations`：MyBatis XML 映射文件的位置（本项目没用到 XML 方式）
- `spring.data.redis.host/port`：Redis 运行在 localhost 的 6379 端口（Redis 默认端口）

---

## 第六章：前端技术栈详解

### 6.1 Vue 3

Vue 是一个前端框架，核心思想是"组件化"——把页面拆分成一个个独立的组件。

```
一个页面 = 多个组件的组合

登录页（LoginView）
├── 登录表单组件
│   ├── 用户名输入框
│   ├── 密码输入框
│   └── 登录按钮
└── 提示信息组件
```

**Vue 3 的新特性：**
- **Composition API**（组合式 API）：用 `setup()` 或 `<script setup>` 组织代码，比 Vue 2 的 Options API 更灵活
- **更好的 TypeScript 支持**
- **性能更好**：虚拟 DOM 重写了

### 6.2 Vite

Vite 是前端构建工具，负责：
- 启动开发服务器（让你能在浏览器里看页面）
- 热更新（改了代码，浏览器自动刷新，不用手动刷）
- 打包（把开发时的很多文件合并压缩成少量文件，用于部署）

**为什么用 Vite 而不是 Webpack？**
> Vite 比 Webpack 快很多。Webpack 启动项目可能需要 30 秒以上，Vite 通常 2-3 秒。

### 6.3 Vue Router

前端路由库。在单页应用（SPA）中，页面切换不会真正刷新浏览器，而是由 Vue Router 控制显示哪个组件。

```javascript
{ path: '/login', component: LoginView }   // 访问 /login 显示登录页
{ path: '/home', component: HomeView }     // 访问 /home 显示主页
```

### 6.4 package.json

前端的"依赖清单"，类似于后端的 `pom.xml`。

```json
{
  "dependencies": {
    "vue": "^3.5.24",         // Vue 框架
    "vue-router": "^4.5.0"    // 路由
  },
  "devDependencies": {
    "vite": "^7.2.4",         // 构建工具
    "@vitejs/plugin-vue": "^6.0.1"  // Vite 的 Vue 插件
  }
}
```

- **dependencies**：运行时需要的包（部署后也要用）
- **devDependencies**：开发时需要的包（部署后不需要）

---

## 第七章：前端代码逐行解读

### 7.1 入口文件：main.js

```javascript
import { createApp } from 'vue'       // 从 Vue 导入创建应用的函数
import './style.css'                    // 导入全局样式
import App from './App.vue'             // 导入根组件
import router from './router'           // 导入路由配置

createApp(App)      // 创建 Vue 应用实例
    .use(router)    // 安装路由插件
    .mount('#app')  // 挂载到 HTML 中 id="app" 的元素上
```

**解释：**
- 这 4 行代码做了所有初始化工作：创建应用 → 装路由 → 挂到页面上
- `#app` 对应 `index.html` 中的 `<div id="app"></div>`，Vue 会把整个应用渲染到这个 div 里

### 7.2 根组件：App.vue

```vue
<template>
  <router-view />    <!-- 路由出口：当前路由对应的组件会显示在这里 -->
</template>
```

**解释：**
- `App.vue` 是整个应用的"外壳"
- `<router-view />` 是一个占位符。当 URL 是 `/login` 时，这里会显示 `LoginView`；当 URL 是 `/home` 时，这里会显示 `HomeView`

### 7.3 路由配置：router/index.js

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'

const routes = [
  { path: '/', redirect: '/login' },           // 访问根路径，自动跳转到登录页
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/home', name: 'Home', component: HomeView }
]

const router = createRouter({
  history: createWebHistory(),   // 使用 HTML5 History 模式（URL 没有 # 号）
  routes
})

export default router
```

**关键概念：**
- **`createWebHistory()`**：使用 HTML5 的 History API，URL 看起来像普通路径（`/login`），而不是带 `#` 的哈希模式（`/#/login`）
- **`redirect`**：重定向。访问 `/` 时自动跳转到 `/login`

### 7.4 登录页：LoginView.vue

这是前端最核心的文件，实现了登录功能。

```vue
<template>
  <div class="login-page">
    <div class="login-box">
      <h2>用户登录</h2>
      <form @submit.prevent="handleLogin">     <!-- ① 阻止表单默认提交行为 -->
        <div class="input-group">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" required />
          <!-- ② v-model：双向绑定，输入框的值自动同步到 username 变量 -->
        </div>
        <div class="input-group">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" required />
        </div>
        <button type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}   <!-- ③ 模板语法：根据 loading 状态显示不同文字 -->
        </button>
      </form>
      <p v-if="message" :class="['msg', success ? 'success' : 'error']">
        {{ message }}   <!-- ④ v-if：只有 message 有值时才显示这个元素 -->
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'          // ref：创建响应式数据
import { useRouter } from 'vue-router'

const username = ref('')            // 用户名，初始值为空字符串
const password = ref('')            // 密码
const loading = ref(false)          // 是否正在登录中
const message = ref('')             // 提示信息
const success = ref(false)          // 是否成功
const router = useRouter()          // 路由实例，用于页面跳转

async function handleLogin() {
  loading.value = true              // 显示"登录中..."
  message.value = ''

  try {
    // ⑤ 构造请求参数（表单格式）
    const params = new URLSearchParams()
    params.append('username', username.value)
    params.append('password', password.value)

    // ⑥ 发送 HTTP 请求到后端
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    })

    const data = await res.json()   // 解析后端返回的 JSON

    if (data.code === 200) {
      success.value = true
      message.value = data.mes
      setTimeout(() => {
        router.push('/home')        // ⑦ 0.8 秒后跳转到主页
      }, 800)
    } else {
      success.value = false
      message.value = data.mes || '登录失败'
    }
  } catch (err) {
    success.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false           // ⑧ 无论成功失败，都取消 loading 状态
  }
}
</script>
```

**关键概念解释：**

- **① `@submit.prevent`**：Vue 的事件修饰符。`.prevent` 表示阻止表单的默认提交行为（否则页面会刷新）。
- **② `v-model`**：双向数据绑定。用户在输入框里打字，`username` 变量自动更新；反过来，修改 `username` 变量，输入框里的内容也会自动变化。
- **③ `{{ }}`**：Vue 的模板语法（插值表达式），可以在 HTML 中显示变量的值。
- **④ `v-if`**：条件渲染。只有条件为 true 时，这个元素才会出现在页面上。
- **⑤ `URLSearchParams`**：把参数编码成 `username=admin&password=123456` 的格式，这是后端 `@RequestParam` 能识别的格式。
- **⑥ `fetch`**：浏览器内置的 HTTP 请求方法。`/api/auth/login` 会被 Vite 代理到 `http://localhost:8080/auth/login`。
- **⑦ `router.push('/home')`**：编程式导航，跳转到 `/home` 路径。
- **⑧ `finally`**：无论 try 中的代码是否出错，finally 中的代码都会执行。

### 7.5 Vite 代理配置：vite.config.js

```javascript
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',   // 后端地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')  // 去掉 /api 前缀
      }
    }
  }
})
```

**为什么需要代理？**

前端运行在 `localhost:5173`，后端运行在 `localhost:8080`，端口不同，浏览器会阻止前端直接请求后端（同源策略）。

代理的作用：前端请求 `/api/auth/login` → Vite 开发服务器收到 → 转发给 `http://localhost:8080/auth/login` → 把结果返回给前端。

```
前端请求：/api/auth/login
              ↓（Vite 代理）
实际请求：http://localhost:8080/auth/login
              ↓（去掉 /api 前缀）
后端接口：/auth/login
```

---

## 第八章：前后端怎么配合工作的？

### 8.1 完整的登录流程

```
第 1 步：用户打开浏览器，访问 http://localhost:5173
         → Vue Router 判断路径是 /，重定向到 /login
         → 显示 LoginView 组件（登录页面）

第 2 步：用户输入用户名 admin，密码 123456，点击"登录"按钮
         → 触发 handleLogin() 函数

第 3 步：handleLogin() 用 fetch 发送 POST 请求到 /api/auth/login
         → Vite 代理把请求转发到 http://localhost:8080/auth/login
         → 请求格式：username=admin&password=123456

第 4 步：后端 Spring Security 的过滤器先检查：这个接口允许匿名访问吗？
         → /auth/login 配置了 permitAll()，放行

第 5 步：请求到达 AuthController.login() 方法
         → 调用 UserService.findUsername("admin")
         → UserService 用 QueryWrapper 构建查询条件
         → UserMapper 执行 SQL：SELECT * FROM sys_user WHERE username='admin'
         → 返回 User 对象

第 6 步：后端用 BCrypt 验证密码
         → passwordEncoder.matches("123456", "$2a$10$xxx...")
         → BCrypt 把 "123456" 用同样的盐加密，比较结果是否一致
         → 一致！密码正确

第 7 步：后端返回 JSON：
         { "code": 200, "mes": "登陆成功", "data": { "nickname": "超级管理员" } }

第 8 步：前端收到响应
         → data.code === 200，登录成功
         → 显示"登陆成功"提示
         → 0.8 秒后 router.push('/home')，跳转到主页
```

### 8.2 数据流向图

```
┌─────────────────┐
│   用户（浏览器）  │
└────────┬────────┘
         │ 输入用户名密码，点击登录
         ▼
┌─────────────────┐
│  LoginView.vue  │  ← 前端：收集用户输入
│  (Vue 组件)      │
└────────┬────────┘
         │ fetch POST /api/auth/login
         ▼
┌─────────────────┐
│  Vite 代理       │  ← 转发请求到后端
│  (vite.config)   │
└────────┬────────┘
         │ POST http://localhost:8080/auth/login
         ▼
┌─────────────────┐
│ SecurityFilter  │  ← 检查权限：/auth/login 允许匿名访问
│ (Spring Security)│
└────────┬────────┘
         │ 放行
         ▼
┌─────────────────┐
│ AuthController  │  ← 接收请求，调用 Service
└────────┬────────┘
         │ 调用 userService.findUsername("admin")
         ▼
┌─────────────────┐
│  UserService    │  ← 构建查询条件
└────────┬────────┘
         │ 调用 userMapper.selectOne(wrapper)
         ▼
┌─────────────────┐
│  UserMapper     │  ← 执行 SQL 查询
│  (MyBatis-Plus) │
└────────┬────────┘
         │ SELECT * FROM sys_user WHERE username='admin'
         ▼
┌─────────────────┐
│    MySQL        │  ← 返回数据
│  (数据库)        │
└─────────────────┘
```

---

## 第九章：为什么这么写？有什么好处？

### 9.1 分层架构（Controller → Service → Mapper）

**为什么这么写？**

这叫"三层架构"，是 Java Web 开发的标准模式：

```
Controller（控制器层）  → 接收请求，返回结果
       ↓ 调用
Service（服务层）       → 处理业务逻辑
       ↓ 调用
Mapper（数据访问层）    → 操作数据库
```

**好处：**
1. **职责清晰**：每一层只干一件事，代码不会乱成一团
2. **易于维护**：改数据库操作不影响 Controller，改页面不影响 Service
3. **易于测试**：可以单独测试某一层，不用启动整个应用
4. **团队协作**：不同的人可以负责不同的层

### 9.2 使用 MyBatis-Plus 而不是原生 JDBC

**原生 JDBC 写法（繁琐）：**
```java
Connection conn = DriverManager.getConnection(url, user, password);
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM sys_user WHERE username = ?");
stmt.setString(1, username);
ResultSet rs = stmt.executeQuery();
if (rs.next()) {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    // ... 每个字段都要手动赋值
}
rs.close();
stmt.close();
conn.close();  // 还要记得关闭连接！
```

**MyBatis-Plus 写法（简洁）：**
```java
wrapper.eq("username", username);
return userMapper.selectOne(wrapper);
```

**好处：**
- 代码量减少 80% 以上
- 不用手动管理连接、关闭资源
- 防止 SQL 注入（参数自动转义）
- 支持条件构造器，动态拼接查询条件很方便

### 9.3 使用 BCrypt 加密密码

**不加密（明文存储）：**
```
数据库中：password = "123456"
→ 数据库被偷，所有用户密码泄露
→ 很多人多个网站用同一个密码，后果严重
```

**BCrypt 加密：**
```
数据库中：password = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
→ 数据库被偷，黑客拿到的只是一堆乱码
→ BCrypt 是单向的，无法反推
→ 还有"盐"机制，防止"彩虹表"攻击
```

### 9.4 前后端通过 JSON 通信

**为什么用 JSON 而不是 XML？**
- JSON 更轻量，格式更简洁
- JavaScript 原生支持 JSON（`JSON.parse()` / `JSON.stringify()`）
- 前后端分离的主流选择

### 9.5 使用 `@RequestParam` 而不是 `@RequestBody`

本项目登录接口用的是 `@RequestParam`（表单格式），而不是 `@RequestBody`（JSON 格式）。

**原因：**
- 前端用 `URLSearchParams` 发送的是 `application/x-www-form-urlencoded` 格式
- `@RequestParam` 正好匹配这种格式
- 这种方式和传统表单提交兼容

---

## 第十章：有没有其他方案？

### 10.1 后端框架的选择

| 方案 | 特点 | 适用场景 |
|------|------|---------|
| **Spring Boot**（本项目） | Java 生态最流行的框架，企业级首选 | 企业项目、大型系统 |
| **Spring MVC** | Spring Boot 的前身，需要大量 XML 配置 | 学习理解原理 |
| **Quarkus** | 新一代 Java 框架，启动极快，内存占用小 | 云原生、微服务 |
| **Node.js + Express** | JavaScript 后端，轻量灵活 | 小项目、全栈 JS |
| **Python + Django/Flask** | 开发速度快，适合快速原型 | AI 相关项目、快速开发 |
| **Go + Gin** | 性能极高，并发能力强 | 高并发场景 |

### 10.2 ORM 框架的选择

| 方案 | 特点 |
|------|------|
| **MyBatis-Plus**（本项目） | 在 MyBatis 基础上增强，简单 CRUD 不用写 SQL |
| **MyBatis** | 需要自己写 SQL，灵活但繁琐 |
| **JPA/Hibernate** | 全自动 ORM，完全不用写 SQL，但复杂查询不灵活 |
| **原生 JDBC** | 最底层，完全手动，不推荐在实际项目中使用 |

### 10.3 前端框架的选择

| 方案 | 特点 |
|------|------|
| **Vue 3**（本项目） | 上手简单，中文文档丰富，国内流行 |
| **React** | 全球最流行，生态最大，灵活但学习曲线陡 |
| **Angular** | 大而全，自带一切，适合大型企业项目 |
| **原生 HTML/JS** | 不用框架，适合简单页面 |

### 10.4 认证方式的选择

本项目只做了简单的用户名密码校验，没有使用 Token 机制。

| 方案 | 特点 |
|------|------|
| **本项目的方式** | 最简单，但没有"记住登录状态"的功能 |
| **Session + Cookie** | 传统方式，服务端存储会话信息 |
| **JWT（JSON Web Token）** | 无状态认证，适合分布式系统，最流行的方案 |
| **OAuth2** | 第三方登录（微信登录、GitHub 登录等） |
| **Spring Security + JWT** | 企业级标准方案，实习面试常考 |

### 10.5 构建工具的选择

| 方案 | 特点 |
|------|------|
| **Vite**（本项目） | 极快的开发服务器，基于 ES Module |
| **Webpack** | 老牌构建工具，生态最丰富 |
| **Rollup** | 适合打包库（Library），不适合应用 |

---

## 第十一章：项目还缺少什么？

### 11.1 功能层面

| 缺失功能 | 说明 | 重要程度 |
|----------|------|---------|
| **注册功能** | 目前只有登录，没有注册新用户的接口 | 高 |
| **Token 认证** | 登录后没有返回 Token，每次请求都需要重新验证身份 | 高 |
| **退出登录** | 前端的"退出登录"只是跳转到登录页，没有清除后端的登录状态 | 高 |
| **密码修改** | 没有修改密码的功能 | 中 |
| **用户管理** | 管理员无法查看、编辑、删除用户列表 | 中 |
| **表单验证** | 前端没有做输入验证（如密码长度、特殊字符限制） | 中 |
| **Redis 缓存** | 引入了 Redis 但没有实际使用 | 低 |

### 11.2 安全层面

| 缺失项 | 说明 | 重要程度 |
|--------|------|---------|
| **JWT/Token 机制** | 没有 Token，无法实现无状态认证 | 高 |
| **接口限流** | 没有限制登录失败次数，可以被暴力破解 | 高 |
| **SQL 注入防护** | 虽然 MyBatis-Plus 自带防护，但应确认所有接口都安全 | 高 |
| **XSS 防护** | 没有对用户输入进行转义 | 中 |
| **HTTPS** | 开发环境用 HTTP，生产环境必须用 HTTPS | 高 |
| **敏感信息保护** | 数据库密码直接写在配置文件中，应该用环境变量或加密配置 | 中 |

### 11.3 工程质量层面

| 缺失项 | 说明 | 重要程度 |
|--------|------|---------|
| **单元测试** | 没有任何测试代码 | 高 |
| **接口文档** | 没有 Swagger/OpenAPI 文档 | 中 |
| **日志系统** | 没有使用 Logback/Log4j 记录日志 | 中 |
| **异常处理** | 没有全局异常处理器（`@ControllerAdvice`） | 高 |
| **统一返回格式** | 应该封装统一的 Result 类，而不是每次都 new HashMap | 中 |
| **参数校验** | 应该用 `@Valid` + `@NotBlank` 等注解做参数校验 | 中 |
| **分页查询** | 列表查询应该支持分页 | 中 |
| **数据库迁移工具** | 应该用 Flyway/Liquibase 管理数据库版本 | 低 |

### 11.4 部署层面

| 缺失项 | 说明 |
|--------|------|
| **Docker 配置** | 没有 Dockerfile，无法容器化部署 |
| **CI/CD** | 没有自动化构建和部署流程 |
| **Nginx 配置** | 生产环境需要 Nginx 做反向代理和静态资源服务 |
| **多环境配置** | 只有开发环境配置，缺少测试、生产环境配置 |

### 11.5 具体改进示例

**1. 统一返回格式（替代 HashMap）：**

```java
// 定义统一的返回类
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
}

// Controller 中就可以这样写：
@PostMapping("/login")
public Result<?> login(String username, String password) {
    // ...
    return Result.success(Map.of("nickname", user.getNickname()));
}
```

**2. 全局异常处理：**

```java
@RestControllerAdvice   // 全局异常处理器
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)   // 捕获所有异常
    public Result<?> handleException(Exception e) {
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}
```

**3. JWT Token 认证（核心代码）：**

```java
// 登录成功后生成 Token
String token = Jwts.builder()
    .setSubject(user.getUsername())
    .setExpiration(new Date(System.currentTimeMillis() + 86400000))  // 24小时过期
    .signWith(SignatureAlgorithm.HS256, "secretKey")
    .compact();

// 返回给前端
return Result.success(Map.of("token", token, "nickname", user.getNickname()));

// 前端每次请求都在 Header 中带上 Token
headers: { 'Authorization': 'Bearer ' + token }
```

---

## 第十二章：实习需要知道的额外知识

### 12.1 面试高频问题

**Q1：Spring Boot 的自动配置原理是什么？**
> 1. `@SpringBootApplication` 包含 `@EnableAutoConfiguration`
> 2. Spring Boot 读取 `META-INF/spring.factories` 文件（或 `org.springframework.boot.autoconfigure.AutoConfiguration.imports`）
> 3. 根据 classpath 中存在的类，判断是否需要启用某个自动配置
> 4. 比如 classpath 中有 MySQL 驱动，就自动配置 DataSource

**Q2：Spring 的 IOC 和 DI 是什么？**
> - **IOC（控制反转）**：对象的创建权从程序员手中转移到了 Spring 容器。以前你 `new User()`，现在 Spring 帮你创建。
> - **DI（依赖注入）**：Spring 自动把对象需要的依赖（其他对象）注入进去。就是 `@Autowired` 做的事情。

**Q3：Spring 的 AOP 是什么？**
> - **AOP（面向切面编程）**：在不修改原有代码的情况下，给方法添加额外功能。
> - 比如：日志记录、事务管理、权限检查。
> - 原理：动态代理。Spring 会为目标类创建一个代理对象，在调用前后执行切面逻辑。

**Q4：MyBatis 的 #{} 和 ${} 有什么区别？**
> - `#{}`：预编译，参数会被当作字符串处理（安全，防 SQL 注入）
> - `${}`：直接拼接，参数会被原样放入 SQL（危险，可能被 SQL 注入）
> - 永远优先使用 `#{}`

**Q5：MySQL 索引是什么？**
> - 索引就像书的目录，帮助数据库快速找到数据。
> - 没有索引：全表扫描（一行一行找），数据量大时非常慢。
> - 有索引：先查目录，直接定位到数据位置。
> - 常用索引类型：B+Tree 索引、哈希索引、全文索引。

**Q6：Redis 为什么快？**
> 1. 数据在内存中，内存读写速度远超磁盘
> 2. 单线程模型，避免了上下文切换和锁竞争
> 3. 高效的数据结构（如跳表、压缩列表等）

**Q7：什么是 RESTful API？**
> - 一种接口设计风格：
>   - `GET /users`：获取用户列表
>   - `GET /users/1`：获取 ID 为 1 的用户
>   - `POST /users`：创建用户
>   - `PUT /users/1`：更新 ID 为 1 的用户
>   - `DELETE /users/1`：删除 ID 为 1 的用户
> - 用 HTTP 方法表示操作类型，用 URL 表示资源

**Q8：Git 常用命令？**
```bash
git init              # 初始化仓库
git add .             # 暂存所有更改
git commit -m "msg"   # 提交
git push              # 推送到远程
git pull              # 拉取最新代码
git branch feature    # 创建分支
git checkout feature  # 切换分支
git merge feature     # 合并分支
```

### 12.2 开发工具

| 工具 | 用途 |
|------|------|
| **IntelliJ IDEA** | Java 开发 IDE（本项目用的就是这个） |
| **VS Code** | 前端开发 IDE |
| **Navicat / DataGrip** | 数据库可视化管理工具 |
| **Postman / Apifox** | 接口测试工具 |
| **Git** | 版本控制 |
| **Maven** | Java 依赖管理和构建 |
| **Docker** | 容器化部署 |

### 12.3 实习中常见的开发流程

```
1. 需求评审   → 产品经理讲解要做什么功能
2. 技术方案   → 讨论怎么实现，数据库怎么设计
3. 接口定义   → 前后端约定接口格式（用 Swagger/Apifox 文档）
4. 开发       → 前端和后端并行开发
5. 联调       → 前后端对接，调试接口
6. 测试       → QA 测试，修复 Bug
7. Code Review → 同事审查你的代码
8. 部署上线   → 发布到测试环境/生产环境
```

### 12.4 实习建议

1. **先把这个项目完善好**：加上注册、JWT 认证、统一返回格式、全局异常处理
2. **学会用 Git**：每天把代码提交到 GitHub，这就是你的简历
3. **刷算法题**：LeetCode 简单题刷 100 道，面试必考
4. **理解 HTTP 协议**：请求方法、状态码、Header、Cookie、Session
5. **学会看官方文档**：Spring、Vue、MySQL 的官方文档是最好的学习资料
6. **做第二个项目**：比如一个增删改查的管理系统（用户管理、商品管理等），展示 CRUD 能力

### 12.5 推荐学习路线

```
第 1 阶段：基础
├── Java 基础语法（变量、循环、类、接口）
├── HTML + CSS + JavaScript 基础
└── MySQL 基础（增删改查 SQL）

第 2 阶段：框架
├── Spring Boot（本项目的核心）
├── MyBatis-Plus（数据库操作）
├── Vue 3（前端框架）
└── Spring Security（安全框架）

第 3 阶段：进阶
├── Redis（缓存）
├── JWT Token（认证）
├── Docker（部署）
├── 消息队列（RabbitMQ/Kafka）
└── 微服务（Spring Cloud）

第 4 阶段：面试
├── 数据结构与算法
├── 计算机网络（HTTP、TCP/IP）
├── 操作系统基础
├── 数据库原理（事务、索引、锁）
└── 设计模式（单例、工厂、观察者等）
```

---

## 附录：常见错误排查

| 错误 | 原因 | 解决方法 |
|------|------|---------|
| 前端报"跨域错误" | 后端没有配置 CORS | 检查 SecurityConfig 中的 CORS 配置 |
| 前端报"Network Error" | 后端没启动 | 先启动 Spring Boot 后端 |
| 数据库连接失败 | MySQL 没启动或密码错误 | 检查 application.properties 中的数据库配置 |
| "用户不存在" | 数据库中没有这个用户 | 检查 InitAdminRunner 是否正常执行，或手动插入数据 |
| "密码错误" | 密码不匹配 | 确认是用 BCrypt 加密存储的，不能直接比对明文 |
| 前端页面空白 | 路由配置错误 | 检查 router/index.js 中的路径和组件映射 |
| Redis 连接失败 | Redis 没启动 | 启动 Redis 服务，或暂时移除 Redis 依赖 |

---

> 本文档到此结束。建议边看文档边看代码，对照着理解每一行的作用。
> 编程是一门实践技能，看懂了不等于会写了——一定要动手！
