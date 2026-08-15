# Java SpringBoot + MySQL + Redis

一个基于 **Spring Boot 3.2.4** 的前后端分离电商项目，后端整合 MySQL、Redis、ActiveMQ，集成 JWT 认证、邮箱验证码、密码强度检测、接口限流、分布式锁、消息队列等企业级通用能力；前端基于 **Vue 3 + Vite** 提供商城购物、商家后台、管理后台三大端。

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.4 |
| 持久层 | MyBatis-Plus 3.5.9（注解版 + 分页插件，无 XML） |
| 数据库 | MySQL 8.x（utf8mb4，逻辑删除） |
| 缓存 | Redis（Lettuce + Redisson 3.36.0，Cache-Aside） |
| 安全 | Spring Security + JWT（jjwt 0.12.6）+ BCrypt |
| 消息队列 | ActiveMQ（JMS，Queue + Topic） |
| 邮件 | Spring Mail（QQ SMTP / SSL） |
| 限流 | 自定义 `@RateLimit` 注解 + Redis Lua 原子自增 |
| 日志/权限切面 | Spring AOP（`@OperationLog` / `@UserCheck`） |
| 前端 | Vue 3.5 + Vite 7 + Vue Router 4 + Axios |
| 构建/语言 | Maven / Java 20 |

---

## 项目结构

```
Java SpringBoot + MySQL + Redis/
├── Script/
│   └── finalInitSql/                 # 数据库初始化脚本
│       ├── InitDataBase.sql          # 建库建表（DDL，全新环境执行）
│       └── InitRealDataBase.sql      # 初始化业务数据（DML，角色/卖家/分类/商品/优惠券）
├── src/main/java/com/xuwenye/demo/
│   ├── DemoApplication.java          # 启动入口
│   ├── Controller/                   # 控制器层
│   │   ├── Admin/                    # 管理员：用户/订单/商品/卖家/充值审核/VIP 管理
│   │   ├── Category/                 # 商品分类（管理员维护）
│   │   ├── Chat/                     # 买卖双方聊天
│   │   ├── Coupon/                   # 优惠券模板/发放/领券/核销
│   │   ├── Order/                    # 订单创建/支付/取消/退款/管理
│   │   ├── Product/                  # 商品查询/管理/卖家商品管理/卖家管理
│   │   ├── Seller/                   # 卖家店铺/订单/发货
│   │   └── User/                     # 认证/用户/收货地址/充值
│   ├── Entity/                       # 实体类（14 张表对应实体）
│   ├── Mapper/                       # 数据访问层（MyBatis-Plus + 注解 SQL）
│   ├── Service/                      # 业务逻辑层（事务/缓存/锁/MQ 核心）
│   ├── annotation/                   # 自定义注解：@RateLimit / @OperationLog / @UserCheck
│   ├── aspect/                       # AOP 切面：限流/操作日志/用户身份校验
│   ├── common/                       # Result、全局异常、WebMvc 配置
│   ├── config/                       # Security / Redis / ActiveMQ / MyBatis-Plus / 启动初始化
│   ├── dto/                          # 响应 DTO
│   └── util/                         # JWT / 密码强度 / 邮件 / 验证码 / 分布式锁 / Redis 等工具
├── src/main/resources/
│   ├── application.properties        # 主配置（MySQL/Redis/MQ/邮件/自定义开关）
│   ├── application-dev.properties    # 开发环境变量
│   └── application-test.properties   # 测试环境配置
├── src/test/java/                    # 单元/集成/性能测试（缓存雪崩、并发扣库存等）
├── frontend/                         # 前端项目（Vue 3 + Vite）
│   └── src/
│       ├── views/                    # 商城/订单/优惠券/商家/管理员页面
│       ├── router/index.js           # 路由 + 登录/角色权限守卫
│       ├── api/index.js              # Axios 统一封装
│       └── utils/                    # token/auth/request 等工具
└── nginx/                            # 生产部署反向代理示例（限流 + 转发）
```

---

## 核心功能

### 商城（买家端）
- 商品浏览：分页/按分类筛选/详情（多图、卖家、推荐位），**公开接口无需登录**
- 领券中心：浏览可领券模板、自助领取、我的优惠券（可用/已用/已过期）
- 下单：携带优惠券结算、分布式锁扣库存、订单状态流转（待支付→已支付→已发货→已完成→已取消/已退款）
- 收货地址管理（增删改查、设默认）
- 余额充值（提交申请 → 管理员审核 → 余额入账）
- 与卖家实时聊天（会话 + 消息、未读数）
- 个人中心：资料/头像/改密/换绑邮箱/注销/找回账号

### 商家端（ROLE_SELLER / ROLE_VIP_SELLER）
- 店铺信息维护（绑定登录账号）
- 商品管理：新增/编辑/上下架/多图/删除/销量与推荐位（VIP 卖家）
- 订单管理：查看本店订单、发货
- 买家消息会话

### 管理端（ROLE_ADMIN）
- 仪表盘统计（用户/商品/订单/交易额）
- 用户管理：列表/禁用/删除恢复/角色调整/VIP 升级
- 商品与卖家管理：上下架/审核/新增卖家（绑定已有账号）
- 分类维护、优惠券模板创建与批量发放
- 订单管理：发货/完成/状态调整
- 充值审核、操作日志审计

---

## 快速开始（开发环境）

### 前置要求
- JDK 20+、Maven 3.8+
- MySQL 8.x、Redis 6.x+（localhost:6379）、ActiveMQ 5.x（localhost:61616）
- Node.js 18+（仅前端需要）

### 1. 初始化数据库
依次执行 `Script/finalInitSql/` 下两个脚本：
```bash
# 1) 建库建表（会 DROP 旧库，注意备份）
mysql -uroot -p < Script/finalInitSql/InitDataBase.sql
# 2) 初始化业务数据（角色/卖家/分类/商品/图片/优惠券）
mysql -uroot -p < Script/finalInitSql/InitRealDataBase.sql
```

### 2. 配置环境
修改 `src/main/resources/application-dev.properties`：
```properties
DB_PASSWORD=你的MySQL密码
JWT_SECRET=你的JWT密钥
MAIL_USERNAME=你的邮箱@qq.com
MAIL_PASSWORD=你的SMTP授权码
```

### 3. 启动后端
```bash
mvn spring-boot:run
```
- 默认端口 **8080**，数据库 `springbootdb`
- 启动时 `InitAdminRunner` 自动创建管理员 `admin / 123456`
- 可修改 `application.properties` 中 `test.username.status` 控制是否批量生成测试用户

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```
- 默认端口 **5173**，开发代理把 `/api`、`/uploads` 转发到 `http://localhost:8081`（若按下方 nginx 部署则指向 8081；直连后端可改为 `8080`）

### 5.（可选）nginx 网关
将 `nginx/nginx.conf` 部署后监听 **8081**，对 `/api/`、`/uploads/` 反向代理到后端 8080，并叠加基础限流。

> 完整的生产部署步骤见 **[build.md](./build.md)**。

---

## 关键设计

### 事务 + 缓存 + 锁 + MQ 架构
- **事务**：订单创建/支付/取消/退款/发货/完成、库存扣减、余额变更均 `@Transactional`
- **缓存**：用户/商品/分类/商品分页/订单/优惠券等查询均走 **Cache-Aside**；缓存清理统一走 **ActiveMQ 异步**，并带随机过期抖动防雪崩
- **分布式锁**：Redisson RLock（看门狗自动续期），保护库存扣减、余额变更、领券/发券等敏感写操作；领券锁粒度为「用户 + 优惠券」
- **MQ 削峰**：邮件发送、操作日志、缓存刷新、优惠券批量发放全部异步化，批量发券逐用户独立小事务，单用户失败不影响整体

### 安全体系
- 无状态 JWT 认证（OncePerRequestFilter），未认证统一返回 401 JSON
- 双层输入过滤（Servlet Filter + HandlerInterceptor）+ 参数清洗防 XSS/SQL 注入
- BCrypt 密码加密 + 密码强度检测
- 自定义 `@RateLimit` 接口限流（IP + 接口维度，Redis Lua 原子自增，异常降级放行）
- 文件上传白名单校验（jpg/png/gif/webp）+ UUID 重命名
- 前后端双重角色权限校验（路由守卫 + 后端 `@UserCheck` 切面）

### 防超卖/防重复领券
- 库存扣减：`UPDATE ... SET stock = stock - ? WHERE stock >= ?` 原子 SQL + 分布式锁
- 领券：分布式锁 + Redis Set 已领取集合 O(1) 预校验 + 原子扣减剩余量 + 数据库唯一索引 `uk_user_coupon(user_id, coupon_id)` 兜底

---

## 数据库表（14 张）

| 表名 | 说明 |
|---|---|
| `sys_user` | 用户（角色/密码/邮箱/余额/状态） |
| `sys_role` | RBAC 角色 |
| `sys_category` | 商品分类（管理员维护） |
| `sys_product` | 商品（分类ID集合、库存、推荐位） |
| `product_image` | 商品图片（多图、主图） |
| `sys_seller` | 卖家（`user_id` 唯一索引与用户一对一绑定） |
| `sys_order` | 订单（状态机、金额） |
| `sys_order_item` | 订单项（商品快照） |
| `sys_operation_log` | 操作日志（AOP + MQ 异步写入） |
| `sys_user_address` | 收货地址 |
| `sys_recharge_request` | 充值申请（管理员审核） |
| `sys_coupon` | 优惠券模板（普通券/VIP 券） |
| `sys_user_coupon` | 用户优惠券（快照 + 唯一索引） |
| `sys_conversation` / `sys_chat_message` | 买卖会话与聊天消息 |

---

## 测试

```bash
mvn test
```
包含：控制器/服务层单测、工具类单测、**缓存雪崩测试**、**库存扣减并发性能测试**（验证分布式锁 + 原子 SQL 防超卖）。

---

## 常见问题

| 问题 | 解决 |
|---|---|
| 启动报 Redis 连接失败 | 确认 Redis 在 localhost:6379 运行；改过序列化/实体后清空旧缓存 `FLUSHALL` |
| 数据库表创建失败 | 按顺序执行 `InitDataBase.sql` → `InitRealDataBase.sql` |
| ActiveMQ 连接失败 | 启动 ActiveMQ（默认 admin/admin）；不需要可注释 pom 中 activemq 依赖 |
| 前端 404/白屏 | `npm install` 后 `npm run dev`；确认代理端口与后端/nginx 一致 |
| 商品图片上传失败 | 确认 `app.upload.dir` 目录存在且有写权限（默认 `./uploads/`） |
