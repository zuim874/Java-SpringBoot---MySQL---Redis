# build.md — 部署步骤

本文档说明如何从零部署本项目（后端 + 前端 + 中间件）。整体架构：

```
浏览器 ──▶ nginx(8081) ──▶ Spring Boot(8080) ──▶ MySQL(3306)
                │                 │
                │                 ├──▶ Redis(6379)     缓存/限流/分布式锁
                │                 └──▶ ActiveMQ(61616) 异步消息
                └──▶ 前端静态资源(dist)
```

**端口规划**

| 服务 | 端口 | 说明 |
|---|---|---|
| MySQL | 3306 | 主数据库（库名 springbootdb） |
| Redis | 6379 | 缓存、限流计数、分布式锁 |
| ActiveMQ | 61616 | 消息队列（web 控制台 8161） |
| Spring Boot | 8080 | 后端 API |
| nginx | 8081 | 反向代理 + 静态资源（可选） |
| Vite dev | 5173 | 仅前端开发时使用 |

---

## 一、环境准备

- JDK 20+（编译与运行）
- Maven 3.8+
- Node.js 18+（构建前端）
- 服务器或本机：Windows / Linux 均可

---

## 二、MySQL 部署与初始化

### 1. 安装并启动 MySQL 8.x

Windows：安装 MySQL Community Server，用 `net start mysql` 启动服务。
Linux：`systemctl start mysqld`。

确认可用：

```bash
mysql -uroot -p
```

### 2. 初始化数据库（重要）

本项目的 SQL 位于 `Script/finalInitSql/` 目录，**必须按顺序执行两个脚本**：

```bash
# ① 建库建表（DDL）
#    注意：脚本开头会 DROP DATABASE IF EXISTS springbootdb，执行前请确认环境/备份
mysql -uroot -p < Script/finalInitSql/InitDataBase.sql

# ② 初始化业务数据（DML：角色/卖家/分类/商品/图片/优惠券）
mysql -uroot -p < Script/finalInitSql/InitRealDataBase.sql
```

初始化后数据概览：

- 角色 5 种：`ROLE_ADMIN / ROLE_USER / ROLE_VIP_USER / ROLE_SELLER / ROLE_VIP_SELLER`
- 卖家 22 个、商品分类 15 个、商品 65 个、优惠券模板 4 个
- **不含用户账号**：管理员 `admin/123456` 由后端启动时 `InitAdminRunner` 自动创建；
  卖家登录账号由 `InitSellerAccountsRunner` 按「店铺名=用户名」匹配已有账号补填绑定

### 3. 创建数据库账号（可选）

建议为项目单独建账号并授权：

```sql
CREATE USER 'demo'@'localhost' IDENTIFIED BY 'Demo@123456';
GRANT ALL PRIVILEGES ON springbootdb.* TO 'demo'@'localhost';
FLUSH PRIVILEGES;
```

---

## 三、Redis 部署

### 1. 安装并启动

Windows：解压 Redis（如 tporadowski/redis），运行：

```bash
redis-server.exe --port 6379
```

Linux：

```bash
# Ubuntu/Debian
apt install redis-server
systemctl start redis
```

### 2. 验证

```bash
redis-cli ping   # 返回 PONG 即正常
```

### 3. 说明

- 本项目 Redis 默认无密码（`application.properties` 中 password 为空）
- 若设置密码，需同步修改 `spring.data.redis.password`
- **每次修改实体序列化结构 / 缓存 key 格式后，建议清空旧缓存**，避免反序列化报错：
  ```bash
  redis-cli FLUSHALL
  ```

---

## 四、ActiveMQ 部署

### 1. 下载并解压

从 Apache 官网下载 ActiveMQ（5.x，如 `apache-activemq-5.18.x-bin.zip`）并解压。

### 2. 启动

Windows：

```bash
bin\activemq start
```

Linux：

```bash
bin/activemq start
```

### 3. 验证

- 消息端口：`tcp://localhost:61616`
- Web 控制台：`http://localhost:8161`，默认账号密码 `admin / admin`

### 4. 配置对齐

后端默认连接配置（`application.properties`）：

```properties
spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin
```

> 若修改了 ActiveMQ 的账号密码，需同步修改后端配置。

---

## 五、后端部署

### 1. 修改环境配置

编辑 `src/main/resources/application-dev.properties`（开发）或通过环境变量注入：

```properties
# MySQL 密码
DB_PASSWORD=你的密码
# JWT 签名密钥（建议随机长字符串，勿泄露）
JWT_SECRET=你的长随机密钥
# 发件邮箱（QQ 邮箱示例）
MAIL_USERNAME=你的邮箱@qq.com
# 邮箱 SMTP 授权码（QQ 邮箱-设置-账户-开启 SMTP 获取）
MAIL_PASSWORD=你的授权码
```

也可直接在 `application.properties` 中修改：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/springbootdb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:}
server.port=8080
```

### 2. 关键配置开关（application.properties）

| 配置项 | 默认 | 说明 |
|---|---|---|
| `test.username.status` | true | 启动时是否批量生成测试用户（建议生产关掉） |
| `test.passwordstrength.status` | true | 注册时是否做密码强度校验 |
| `test.registerCodeCheck.status` | true | 注册时是否校验邮箱验证码 |
| `test.recoverCodeCheck.status` | true | 找回账号时是否校验验证码 |
| `test.deleteCodeCheck.status` | true | 注销时是否校验验证码 |
| `app.upload.dir` | ./uploads/ | 文件上传根目录 |

### 3. 打包

```bash
mvn clean package -DskipTests
```

产物：`target/Java-SpringBoot-MySQL-Redis-1.0-SNAPSHOT.jar`

> 建议先本地跑一遍测试确认无误：`mvn test`（测试需要 MySQL/Redis/ActiveMQ 已启动）。

### 4. 运行

```bash
java -jar target/Java-SpringBoot-MySQL-Redis-1.0-SNAPSHOT.jar
```

首次启动会自动：
- 创建管理员账号 `admin / 123456`
- 为存量卖家补填 `user_id` 账号绑定

验证：

```bash
curl http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

返回 `code=200` 且带 `token` 即部署成功。

### 5. 前台启动 / 后台运行（Linux）

```bash
nohup java -jar app.jar > app.log 2>&1 &
```

---

## 六、前端构建与部署

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 构建生产包

```bash
npm run build
```

产物输出到 `frontend/dist/`。

### 3. 部署静态资源

将 `frontend/dist/` 内容拷贝到 nginx 静态目录（如 `D:/nginx/html/dist`）或任意 Web 服务目录。

> 生产环境前端请求经 nginx 反向代理到后端（见第七节），无需额外配置跨域；
> 若直连后端且前后端不同源，需依赖后端已开启的 CORS 配置。

---

## 七、nginx 反向代理（可选但推荐）

项目提供示例配置 `nginx/nginx.conf`：

- 监听 **8081**
- `/api/` → 反向代理到 `127.0.0.1:8080`（后端）
- `/uploads/` → 反向代理到后端静态资源
- 叠加基础限流（600 次/分钟 + burst 100，超出返回 429）

### 部署步骤

1. 安装 nginx；
2. 修改 `nginx.conf`（参考 `nginx/nginx.conf`，注意 `location /upload/ { alias D:/upload/; }` 需改成你本机的上传目录）；
3. 启动：

```bash
nginx -t          # 校验配置
nginx             # 启动
```

### 验证

```bash
curl http://localhost:8081/api/category/list
```

返回 JSON 即代理正常。

> **注意前后端端口一致性**：`frontend/vite.config.js` 开发代理默认转发到 `http://localhost:8081`（即 nginx），
> 若未部署 nginx，请把代理目标改为后端 `http://localhost:8080`。

---

## 八、整体启动顺序（Checklist）

```text
1. 启动 MySQL 3306          ✅ 已初始化 springbootdb
2. 启动 Redis 6379          ✅ redis-cli ping → PONG
3. 启动 ActiveMQ 61616      ✅ 控制台 8161 可访问
4. （可选）清空旧缓存       ✅ redis-cli FLUSHALL（换过序列化结构时）
5. 启动后端 8080            ✅ /api/auth/login 返回 token
6. （可选）部署 nginx 8081  ✅ /api/category/list 返回 JSON
7. 构建并部署前端 dist      ✅ 浏览器访问页面正常
```

---

## 九、常见部署问题

| 现象 | 排查 |
|---|---|
| 启动报 `Unable to connect to Redis` | Redis 未启动或密码/端口不对；`redis-cli ping` 排查 |
| 启动报 MySQL 连接失败 | 数据库未建 / 账号密码不对 / 未执行 `InitDataBase.sql` |
| 启动报 ActiveMQ 连接失败 | ActiveMQ 未启动或 broker-url 不对；不需要可注释 pom 的 activemq 依赖 |
| 反序列化 `ClassCastException` / `LinkedHashMap` 报错 | Redis 里有旧格式缓存，`FLUSHALL` 后重启 |
| 页面白屏 / 接口 404 | 前端代理端口与后端/nginx 不一致；检查 `vite.config.js` 与 `nginx.conf` |
| 邮件发不出去 | 检查 `MAIL_USERNAME/MAIL_PASSWORD` 是否为 QQ 邮箱 SMTP 授权码（非登录密码） |
| 图片上传失败 | 检查 `app.upload.dir`（默认 `./uploads/`）目录存在且有写权限 |
