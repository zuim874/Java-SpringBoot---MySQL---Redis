# Java SpringBoot + MySQL + Redis

一个基于 **Spring Boot 3.2.4** 的企业级 Web 项目，整合 MySQL 数据库和 Redis 缓存，集成 JWT 认证、邮箱验证码、密码强度检测、安全过滤、商品管理、订单系统、消息队列等通用功能。

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
| 消息队列 | ActiveMQ |
| 前端 | Vue 3 + Vite |
| 构建 | Maven |
| Java | 20 |

---

## 项目结构

```
src/main/java/com/xuwenye/demo/
├── DemoApplication.java                # 启动入口
├── Controller/                         # 控制器层
│   ├── AuthController.java             # 登录、注册、发送注册验证码
│   ├── UserController.java             # 用户删除/恢复、发送恢复验证码
│   ├── ProductController.java          # 商品查询、详情（公开接口 + 管理接口）
│   ├── OrderController.java            # 订单创建、支付、取消、查询
│   ├── AdminController.java            # 管理员面板（仪表盘数据、用户管理、商品审核）
│   └── SellerProductManageController.java # 商家商品管理（CRUD、上下架、图片管理）
├── Entity/                             # 实体类
│   ├── User.java                       # 用户表 sys_user
│   ├── Role.java                       # 角色表 sys_role
│   ├── Product.java                    # 商品表 sys_product
│   ├── ProductImage.java               # 商品图片表 sys_product_image
│   ├── Seller.java                     # 商家表 sys_seller
│   ├── Order.java                      # 订单表 sys_order
│   ├── OrderItem.java                  # 订单项表 sys_order_item
│   └── TaskMessage.java                # 消息队列任务实体
├── Mapper/                             # 数据访问层
│   ├── UserMapper.java                 # 用户 Mapper + 自定义 SQL
│   ├── RoleMapper.java                 # 角色 Mapper
│   ├── ProductMapper.java              # 商品 Mapper（分页、分类、自定义查询）
│   ├── ProductImageMapper.java         # 商品图片 Mapper
│   ├── SellerMapper.java               # 商家 Mapper
│   ├── OrderMapper.java                # 订单 Mapper
│   └── OrderItemMapper.java            # 订单项 Mapper
├── Service/                            # 业务逻辑层
│   ├── UserService.java                # 用户业务 + Redis 缓存
│   ├── RoleService.java                # 角色业务（预留）
│   ├── ProductService.java             # 商品业务（缓存、分布式锁、分页）
│   ├── SellerService.java              # 商家业务
│   ├── OrderService.java               # 订单业务（下单、支付、取消、退款）
│   ├── FileStorageService.java         # 文件上传服务（商品图片、格式校验）
│   ├── MQProducer.java                 # 消息队列生产者
│   └── MQConsumer.java                 # 消息队列消费者（邮件、日志、通知）
├── common/                             # 公共组件
│   ├── Result.java                     # 统一 JSON 返回格式（code/mes/data）
│   ├── GlobalExceptionHandler.java     # 全局异常处理
│   ├── GlobalSafeFilter.java           # Servlet 安全过滤器
│   ├── SqlInjectSafeInterceptor.java   # SQL 注入拦截器
│   └── WebMvcConfig.java               # Web MVC 配置
├── config/                             # 配置类
│   ├── SecurityConfig.java             # Spring Security + CORS + JWT + 接口权限
│   ├── JwtAuthenticationFilter.java    # JWT 认证过滤器
│   ├── RedisConfig.java                # RedisTemplate + Redisson
│   ├── RedisLockHelper.java            # Redisson 分布式锁工具
│   ├── InitAdminRunner.java            # 启动时初始化管理员 + 测试账号
│   ├── PasswordStrengthConfig.java     # 密码强度检测配置
│   ├── MyBatisPlusConfig.java          # MyBatis-Plus 分页插件
│   └── activeMQ/
│       └── ActiveMQConfig.java         # ActiveMQ 消息队列配置（Queue、Topic、JmsTemplate）
├── annotation/                         # 自定义注解
│   └── RateLimit.java                  # 接口限流注解（IP + 用户维度）
├── aspect/                             # 切面
│   └── RateLimitAspect.java            # 限流切面实现（Redis + Lua 脚本）
└── util/                               # 工具类
    ├── auth/
    │   └── JwtUtil.java                # JWT 生成 / 解析 / 校验
    ├── redis/
    │   ├── RedisUtil.java              # Redis 操作封装
    │   └── RedisLockHelper.java        # 分布式锁工具类
    ├── EmailUtil.java                  # 异步邮件发送（验证码）
    ├── PasswordStrengthUtils.java      # 密码强度评分引擎
    └── SanitizeUtil.java               # XSS + SQL 注入输入清洗

frontend/                               # 前端项目（Vue 3 + Vite）
├── src/
│   ├── views/
│   │   ├── HomeView.vue                # 首页（商品列表、分类筛选、分页）
│   │   ├── LoginView.vue               # 登录页
│   │   ├── RegisterView.vue            # 注册页
│   │   ├── ProductDetailView.vue       # 商品详情页（购买、评价）
│   │   ├── OrderListView.vue           # 订单列表页
│   │   ├── OrderDetailView.vue         # 订单详情页
│   │   ├── SellerProductManageView.vue # 商家商品管理页
│   │   ├── AdminDashboardView.vue      # 管理员仪表盘
│   │   └── RecoverView.vue             # 账号恢复页
│   ├── router/
│   │   └── index.js                    # 路由配置（含权限守卫）
│   ├── utils/
│   │   ├── auth.js                     # 权限验证工具
│   │   └── tokenChecker.js             # Token 过期检查
│   └── api/
│       └── index.js                    # 统一 API 封装（axios 实例）
```

---

## 快速开始

### 前置要求

- JDK 20+
- Maven 3.8+
- MySQL 8.x
- Redis 6.x+（需在 localhost:6379 运行）
- ActiveMQ（可选，用于消息队列功能）
- Node.js 18+（仅前端开发需要）

### 配置数据库

1. 执行 `Script/finalInitSql/InitDataBase.sql` 创建数据库和基础表结构
2. 执行 `Script/InitProductData.sql` 初始化商品、商家等业务数据
3. 修改 `application-dev.properties` 中的数据库连接信息（默认密码 `123456`）

### 配置邮件（可选）

在 `application-dev.properties` 中配置 QQ 邮箱 SMTP：

```properties
MAIL_USERNAME=your邮箱@qq.com
MAIL_PASSWORD=你的SMTP授权码
```

### 配置 ActiveMQ（可选）

确保 ActiveMQ 服务已在 localhost:61616 运行，默认账号密码 admin/admin。
ActiveMQ 用于异步发送邮件、审计日志、缓存刷新等场景。
如果不需要消息队列功能，可以注释掉 `pom.xml` 中的 ActiveMQ 依赖。

### 启动后端

```bash
mvn spring-boot:run
```

默认端口 **8080**，启动后会自动创建 `admin` 管理员账号和 1000 个测试账号（通过 `test.username.status=true` 控制）。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口 **5173**，开发环境已配置代理转发到后端 8080 端口。

---

## API 接口

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/auth/login` | 登录（返回 token + nickname） | 公开 |
| POST | `/api/auth/register` | 注册（需邮箱验证码） | 公开 |
| POST | `/api/auth/send-registercode` | 发送注册验证码 | 公开 |

### 用户 `/api/user`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| DELETE | `/api/user/delete_admin` | 管理员删除用户 | ADMIN |
| DELETE | `/api/user/delete_user` | 用户注销自己 | 登录用户 |
| PUT | `/api/user/recover_admin` | 管理员恢复用户 | ADMIN |
| PUT | `/api/user/recover_user` | 用户自行恢复（需邮箱验证码） | 公开 |
| POST | `/api/user/send-recovercode` | 发送账号恢复验证码 | 公开 |

### 商品 `/api/product`

| 方法 | 路径 | 说明 | 权限 | 限流 |
|---|---|---|---|---|
| GET | `/api/product/page` | 分页查询上架商品（支持分类筛选） | 公开 | 30次/60s |
| GET | `/api/product/list` | 获取所有上架商品 | 公开 | 30次/60s |
| GET | `/api/product/list/category` | 按分类获取上架商品 | 公开 | 30次/60s |
| GET | `/api/product/detail/{id}` | 获取商品详情（含卖家、图片） | 公开 | 30次/60s |
| GET | `/api/product/categories` | 获取所有分类列表 | 公开 | 30次/60s |
| POST | `/api/product/create` | 新增商品（含图片上传） | ADMIN | 5次/60s |
| PUT | `/api/product/update` | 更新商品信息 | ADMIN | 5次/60s |
| DELETE | `/api/product/delete/{id}` | 逻辑删除商品 | ADMIN | 5次/60s |
| PUT | `/api/product/onshelf/{id}` | 上架商品 | ADMIN | 5次/60s |
| PUT | `/api/product/offshelf/{id}` | 下架商品 | ADMIN | 5次/60s |
| POST | `/api/product/image/upload` | 上传商品图片 | ADMIN | 5次/60s |
| DELETE | `/api/product/image/delete/{id}` | 删除商品图片 | ADMIN | 5次/60s |

### 订单 `/api/order`

| 方法 | 路径 | 说明 | 权限 | 限流 |
|---|---|---|---|---|
| POST | `/api/order/create` | 创建订单（扣减库存） | 登录用户 | 10次/60s |
| POST | `/api/order/pay/{orderNo}` | 支付订单（模拟） | 登录用户 | 10次/60s |
| POST | `/api/order/cancel/{orderNo}` | 取消订单（恢复库存） | 登录用户 | 10次/60s |
| POST | `/api/order/refund/{orderNo}` | 申请退款 | 登录用户 | 5次/60s |
| GET | `/api/order/detail/{orderNo}` | 查询订单详情 | 登录用户 | 30次/60s |
| GET | `/api/order/list` | 查询当前用户订单列表 | 登录用户 | 30次/60s |
| PUT | `/api/order/status` | 管理员更新订单状态 | ADMIN | 10次/60s |

### 商家商品管理 `/api/seller/products`

| 方法 | 路径 | 说明 | 权限 | 限流 |
|---|---|---|---|---|
| GET | `/api/seller/products/list` | 获取商家商品列表 | SELLER/ADMIN | 30次/60s |
| POST | `/api/seller/products/create` | 商家新增商品 | SELLER/ADMIN | 5次/60s |
| PUT | `/api/seller/products/update` | 商家更新商品 | SELLER/ADMIN | 5次/60s |
| DELETE | `/api/seller/products/delete/{id}` | 商家删除商品 | SELLER/ADMIN | 5次/60s |
| PUT | `/api/seller/products/onshelf/{id}` | 商家上架商品 | SELLER/ADMIN | 5次/60s |
| PUT | `/api/seller/products/offshelf/{id}` | 商家下架商品 | SELLER/ADMIN | 5次/60s |

### 管理员面板 `/api/admin`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/admin/dashboard` | 获取仪表盘统计数据 | ADMIN |
| GET | `/api/admin/users` | 分页查询用户列表 | ADMIN |
| PUT | `/api/admin/user/role` | 修改用户角色 | ADMIN |
| GET | `/api/admin/orders` | 分页查询所有订单 | ADMIN |
| PUT | `/api/admin/order/status` | 更新订单状态 | ADMIN |
| GET | `/api/admin/products/pending` | 查询待审核商品 | ADMIN |
| PUT | `/api/admin/product/audit` | 审核商品（通过/驳回） | ADMIN |

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

# ========== ActiveMQ 配置 ==========
spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin
spring.activemq.pool.enabled=true
spring.activemq.pool.max-connections=10

# ========== 文件上传配置 ==========
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB
```

---

## 安全机制

- **JWT 认证**：Spring Security + OncePerRequestFilter，无状态会话
- **双重输入过滤**：Servlet Filter（GlobalSafeFilter）+ HandlerInterceptor（SqlInjectSafeInterceptor）
- **参数清洗**：SanitizeUtil 去除 XSS 和 SQL 注入危险字符
- **密码加密**：BCryptPasswordEncoder
- **邮箱防刷**：Redis 60 秒发送频率限制
- **分布式锁**：Redisson RLock（看门狗模式，用于库存扣减等场景）
- **接口限流**：自定义 @RateLimit 注解 + Redis Lua 脚本，支持 IP 和用户维度限流
- **角色权限控制**：Spring Security 注解（@PreAuthorize）+ 自定义权限校验
- **文件上传校验**：白名单文件类型（jpg/png/gif/webp）+ 大小限制

---

## 核心功能模块

### 商品系统

- 商品 CRUD（含逻辑删除）
- 多分类支持（电子产品、家居用品、图书文具、美妆个护等）
- 商品图片管理（多图上传、主图设置、UUID 文件名）
- Redis 缓存（Cache-Aside 模式，写操作后自动清除缓存）
- 分布式锁保护库存扣减
- 分页查询（MyBatis-Plus 分页插件）
- 商家商品管理（商家可管理自己的商品）

### 订单系统

- 订单创建（含分布式锁库存扣减）
- 订单支付（模拟支付流程）
- 订单取消（恢复库存）
- 退款申请
- 订单状态流转（待支付 → 已支付 → 已发货 → 已完成 → 已取消/已退款）
- 订单编号生成（时间戳 + 随机数）

### 消息队列（ActiveMQ）

- 异步邮件发送（注册验证码、账号恢复通知）
- 用户操作审计日志
- 缓存刷新广播
- 支持 Queue（点对点）和 Topic（发布/订阅）两种模式
- 手动确认机制（CLIENT_ACKNOWLEDGE）

### 管理员面板

- 仪表盘统计（用户数、商品数、订单数、交易额）
- 用户管理（分页查询、角色修改）
- 订单管理（订单状态更新、全量查询）
- 商品审核（待审核商品查询、审核通过/驳回）

### 前端页面

- 首页：商品展示、分类筛选、分页导航
- 商品详情页：商品信息、图片轮播、购买入口
- 订单列表/详情页：订单状态跟踪、支付/取消操作
- 商家商品管理页：商品 CRUD、上下架、图片管理
- 管理员仪表盘：数据统计、用户/订单/商品管理
- 统一的玻璃拟态（Glassmorphism）设计风格
- 路由权限守卫（未登录自动跳转登录页）
- Token 过期自动检测与跳转

---

## 数据库表结构

### 主要业务表

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户表（含角色、密码、邮箱等） |
| `sys_role` | 角色表（admin/user/seller） |
| `sys_product` | 商品表（含分类、库存、价格、销量等） |
| `sys_product_image` | 商品图片表（多图支持、排序、主图标记） |
| `sys_seller` | 商家表（含联系方式、头像等） |
| `sys_order` | 订单表（含订单状态、金额、收货信息等） |
| `sys_order_item` | 订单项表（商品快照、数量、单价等） |

---

## 常见问题

### Q1：启动后报 Redis 连接失败

确保 Redis 服务已在 localhost:6379 运行。
如果之前有旧的序列化数据，建议先执行 `FLUSHALL` 清除缓存。

### Q2：数据库表创建失败

按顺序执行 SQL 文件：
1. 先执行 `Script/finalInitSql/InitDataBase.sql` 创建基础表
2. 再执行 `Script/InitProductData.sql` 初始化业务数据

### Q3：前端页面访问白屏

确保已执行 `npm install` 安装依赖，然后 `npm run dev` 启动前端开发服务器。

### Q4：ActiveMQ 连接失败

如果不需要消息队列功能，可以注释 `pom.xml` 中的 ActiveMQ 相关依赖。
如果需要使用，确保 ActiveMQ 服务已在 localhost:61616 运行。

### Q5：商品图片上传失败

检查 `application.properties` 中的文件上传配置，确保 `file.upload-dir` 指向的目录存在且有写入权限（默认 `./uploads/products/`）。