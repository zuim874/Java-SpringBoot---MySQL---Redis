# Java SpringBoot + MySQL + Redis

一个基于 **Spring Boot 3.2.4** 的后端项目，整合 MySQL 数据库和 Redis 缓存，集成 JWT 认证、邮箱验证码、密码强度检测、安全过滤等通用功能。

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 框架 | Spring Boot 3.2.4 |
| ORM | MyBatis-Plus 3.5.9（注解版，无 XML） |
| 数据库 | MySQL 8.x |
| 缓存 | Redis（Lettuce + Redisson 3.36.0） |
| 安全 | Spring Security + JWT（jjwt 0.12.6） |
| 邮件 | Spring Mail（QQ SMTP / SSL） |
| 构建 | Maven |
| Java | 20 |

---

## 项目结构

```
src/main/java/com/xuweney/demo/
├── DemoApplication.java          # 启动入口
├── Controller/                   # 控制器层
│   ├── AuthController.java       # 登录、注册、发送注册验证码
│   └── UserController.java       # 用户删除/恢复、发送恢复验证码
├── Entity/                       # 实体类
│   ├── User.java                 # 用户表 sys_user
│   └── Role.java                 # 角色表 sys_role
├── Mapper/                       # 数据访问层
│   ├── UserMapper.java           # 用户 Mapper + 自定义 SQL
│   └── RoleMapper.java           # 角色 Mapper
├── Service/                      # 业务逻辑层
│   ├── UserService.java          # 用户业务 + Redis 缓存
│   └── RoleService.java          # 角色业务（预留）
├── common/                       # 公共组件
│   ├── Result.java               # 统一 JSON 返回格式
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   ├── GlobalSafeFilter.java     # Servlet 安全过滤器
│   ├── SqlInjectSafeInterceptor.java # SQL 注入拦截器
│   └── WebMvcConfig.java         # Web MVC 配置
├── config/                       # 配置类
│   ├── SecurityConfig.java       # Spring Security + CORS + JWT
│   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   ├── RedisConfig.java          # RedisTemplate + Redisson
│   ├── RedisLockHelper.java      # Redisson 分布式锁工具
│   ├── InitAdminRunner.java      # 启动时初始化管理员 + 测试账号
│   └── PasswordStrengthConfig.java  # 密码强度检测配置
└── util/                         # 工具类
    ├── JwtUtil.java              # JWT 生成 / 解析 / 校验
    ├── RedisUtil.java            # Redis 操作封装
    ├── EmailUtil.java            # 异步邮件发送（验证码）
    ├── PasswordStrengthUtils.java # 密码强度评分引擎
    └── SanitizeUtil.java         # XSS + SQL 注入输入清洗
```

---

## 快速开始

### 前置要求

- JDK 20+
- Maven 3.8+
- MySQL 8.x
- Redis 6.x+

### 配置数据库

1. 执行 `Script/InitDataBase.sql` 创建数据库和表
2. 修改 `application-dev.properties` 中的数据库连接信息（默认密码 `123456`）

### 配置邮件（可选）

在 `application-dev.properties` 中配置 QQ 邮箱 SMTP：

```properties
MAIL_USERNAME=your邮箱@qq.com
MAIL_PASSWORD=你的SMTP授权码
```

### 启动

```bash
mvn spring-boot:run
```

默认端口 **8080**，启动后会自动创建 `admin` 管理员账号和 1000 个测试账号（通过 `test.username.status=true` 控制）。

---

## API 接口

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录（返回 token + nickname） |
| POST | `/api/auth/register` | 注册（需邮箱验证码） |
| POST | `/api/auth/send-registercode` | 发送注册验证码 |

### 用户 `/api/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| DELETE | `/api/user/delete_admin` | 管理员删除用户（需 token） |
| DELETE | `/api/user/delete_user` | 用户注销自己（需 token） |
| PUT | `/api/user/recover_admin` | 管理员恢复用户（需 token） |
| PUT | `/api/user/recover_user` | 用户自行恢复（需邮箱验证码） |
| POST | `/api/user/send-recovercode` | 发送账号恢复验证码 |

---

## 配置说明

`application.properties` 中的关键开关：

```properties
# 启动时创建管理员 + 1000 个测试用户
test.username.status=true

# 密码强度检测（开启后密码至少 8 位且包含多种字符）
test.passwordstrength.status=false

# 注册 / 恢复时校验邮箱验证码
test.registerCodeCheck.status=false
test.recoverCodeCheck.status=true
```

---

## 安全机制

- **JWT 认证**：Spring Security + OncePerRequestFilter，无状态会话
- **双重输入过滤**：Servlet Filter（GlobalSafeFilter）+ HandlerInterceptor（SqlInjectSafeInterceptor）
- **参数清洗**：SanitizeUtil 去除 XSS 和 SQL 注入危险字符
- **密码加密**：BCryptPasswordEncoder
- **邮箱防刷**：Redis 60 秒发送频率限制
- **分布式锁**：Redisson RLock（预留扩展）
