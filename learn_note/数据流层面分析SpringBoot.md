# 数据流层面分析 SpringBoot 全链路

> 以本项目为例：用户通过浏览器访问 ZuiMShop 商城，执行"登录 → 浏览商品 → 下单 → 查看订单"完整流程，追踪每一条数据从键盘敲击到数据库落盘、再返回屏幕的字节级旅程。

---

## 目录

1. [阶段1：客户端浏览器 — 用户输入与请求构建](#阶段1客户端浏览器--用户输入与请求构建)
2. [阶段2：网络传输 — HTTP 请求包在互联网中的旅程](#阶段2网络传输--http-请求包在互联网中的旅程)
3. [阶段3：服务端接收 — 从网卡到 Tomcat 线程池](#阶段3服务端接收--从网卡到-tomcat-线程池)
4. [阶段4：SpringBoot 核心处理链 — DispatcherServlet 全流程](#阶段4springboot-核心处理链--dispatcherservlet-全流程)
5. [阶段5：业务逻辑层 — Service 与缓存/消息队列](#阶段5业务逻辑层--service-与缓存消息队列)
6. [阶段6：数据持久层 — MyBatis → JDBC → MySQL 存储引擎](#阶段6数据持久层--mybatis--jdbc--mysql-存储引擎)
7. [阶段7：响应返回 — 序列化到浏览器渲染](#阶段7响应返回--序列化到浏览器渲染)
8. [阶段8：异步处理 — ActiveMQ 消息与 Redis 缓存刷新](#阶段8异步处理--activemq-消息与-redis-缓存刷新)
9. [附录：字节级数据大小参考](#附录字节级数据大小参考)

---

## 阶段1：客户端浏览器 — 用户输入与请求构建

### 1.1 键盘输入 → 硬件中断 → 操作系统

用户按下键盘按键（如 `Enter`），触发以下链路：

```
按键
  ↓ 物理信号
键盘扫描矩阵检测到按键闭合
  ↓ 电信号
键盘控制器（Intel 8042 / USB HID）产生扫描码（Make Code）
  ↓ 硬件中断
南桥芯片 → APIC（高级可编程中断控制器）→ CPU 的 LAPIC
  ↓ 
CPU 收到 IRQ1（键盘中断），保存当前上下文（寄存器压栈）
  ↓ 查中断向量表
CPU 读取 IDTR（中断描述符表寄存器），找到中断向量 33（0x21）对应的处理程序
  ↓ 切换到 Ring0（内核态）
执行键盘驱动中断处理程序
```

**字节级细节：**

| 操作 | 数据量 | 说明 |
|------|--------|------|
| 键盘扫描码 | 1 字节 | 例如 `0x1C` 表示 Enter 键的 Make Code |
| 中断向量表项 | 8 字节 (x86) | 包含段选择子 + 偏移量，共 64 位 |
| 上下文切换 | ~72 字节 | 通用寄存器 + 段寄存器 + EFLAGS 压栈 |
| 键盘缓冲区 | 1 字节 | 内核维护的 buffer，由驱动写入 |

### 1.2 进程间通信 — 内核 → 用户态浏览器

```
内核键盘驱动将扫描码翻译为 ASCII 码（如 `0x0D` = CR）
  ↓
写入 /dev/input/eventX（Linux）或 系统消息队列（Windows）
  ↓
内核唤醒等待 read() 的浏览器进程
  ↓ 切换到 Ring3（用户态）
浏览器事件循环收到键盘事件
  ↓
JavaScript 引擎执行 onkeydown/onkeyup 回调
  ↓
Vue 框架 v-model 双向绑定更新响应式数据对象
```

**字节级细节：**

| 操作 | 数据量 | 说明 |
|------|--------|------|
| input_event 结构体 | 16 字节 | `struct input_event { timeval, type, code, value }` |
| 上下文切换（内核→用户） | ~72 字节 | 恢复用户态寄存器 |
| 浏览器事件对象 | ~128 字节 | 包含 keyCode、target、timestamp 等属性 |
| Vue 响应式数据变更 | ~64 字节 | Proxy 的 set 拦截 + 依赖通知 |

### 1.3 HTTP 请求构建 — 从 URL 到网络包

用户在浏览器地址栏输入 `http://localhost:8080/api/auth/login` 并回车，或者前端 Vue 应用通过 Axios 发起 AJAX 请求：

```
fetch('/api/auth/login', { method: 'POST', body: JSON.stringify({username, password}) })
  ↓
Axios 库创建 XMLHttpRequest 对象
  ↓
V8 引擎执行 JavaScript 代码 → 调用 Chromium 网络栈
  ↓
URL 解析 → 协议（HTTP/1.1）、主机（localhost）、端口（8080）、路径（/api/auth/login）
  ↓
DNS 解析（本地查询 localhost → 127.0.0.1，无需网络）
  ↓
构造 HTTP 请求报文
```

**HTTP 请求报文结构（字节级）：**

```
POST /api/auth/login HTTP/1.1\r\n                    ← 请求行 ~30 字节
Host: localhost:8080\r\n                               ← 头部 ~20 字节
Content-Type: application/json\r\n                      ← 头部 ~30 字节
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...\r\n       ← 头部（JWT Token）~200 字节
Content-Length: 52\r\n                                  ← 头部 ~20 字节
\r\n                                                      ← 空行 2 字节
{"username":"admin","password":"123456"}                ← 请求体 ~42 字节
```

**字节级分项：**

| 报文部分 | 字节数 | 说明 |
|----------|--------|------|
| 请求行 `POST /api/auth/login HTTP/1.1` | 31 | 方法 + 空格 + URI + 空格 + 版本 + CRLF |
| Host 头 | 20 | `Host: localhost:8080\r\n` |
| Content-Type 头 | 30 | `Content-Type: application/json\r\n` |
| Authorization 头 | ~200 | `Authorization: Bearer <JWT>\r\n`，JWT 约 180 字节 |
| Content-Length 头 | 22 | `Content-Length: 52\r\n` |
| 空行 | 2 | `\r\n` |
| 请求体 (JSON) | 42 | `{"username":"admin","password":"123456"}` |
| **总计** | **~347 字节** | HTTP 请求报文 |

### 1.4 TCP 连接建立 — 三次握手

在发送 HTTP 请求之前，需要先建立 TCP 连接：

```
客户端（浏览器）                     服务器（Tomcat）
    |                                    |
    |---- SYN (seq=1000) --------------->|  (1) 客户端发送 SYN 包
    |                                    |
    |<--- SYN+ACK (seq=5000, ack=1001) --|  (2) 服务器回复 SYN+ACK
    |                                    |
    |---- ACK (seq=1001, ack=5001) ----->|  (3) 客户端发送 ACK
    |                                    |
    |---- HTTP 请求报文 ----------------->|  (4) 发送 HTTP 数据
```

**字节级细节：**

| TCP 包 | 数据大小 | 头部 | 总大小 |
|--------|---------|------|--------|
| SYN | 0 字节 | 20 字节 (TCP) + 20 字节 (IP) | 40 字节 |
| SYN+ACK | 0 字节 | 20 + 20 字节 | 40 字节 |
| ACK | 0 字节 | 20 + 20 字节 | 40 字节 |
| HTTP 请求 | 347 字节 | 20 + 20 字节 | 387 字节 |
| **总计** | | | **~507 字节** |

**TCP 段结构（20 字节头部）：**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         源端口（16 位）         |       目的端口（16 位）         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       序列号（32 位）                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       确认号（32 位）                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
| 数据偏移 | 保留  |N|C|E|U|A|P|R|S|F|        窗口大小          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         校验和（16 位）         |       紧急指针（16 位）         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

---

## 阶段2：网络传输 — HTTP 请求包在互联网中的旅程

### 2.1 本地开发环境（localhost）

由于是本地开发，请求不经过物理网卡，而是通过 **loopback 接口（lo）**：

```
HTTP 请求报文（347 字节 HTTP + 40 字节 TCP/IP 头部 = 387 字节）
  ↓
TCP 层：分段（MSS = 16396 字节，远大于 387 字节，不分段）
  ↓
IP 层：封装为 IP 数据报（目标 127.0.0.1）
  ↓
数据链路层：封装为本地回环帧（lo 接口，无 MAC 地址）
  ↓
内核协议栈直接将数据从发送缓冲区拷贝到接收缓冲区
  ↓
目标端口 8080 上的 Tomcat 进程被唤醒
```

**字节级细节：**

| 层 | 协议 | 头部大小 | 总大小 |
|----|------|---------|--------|
| 应用层 | HTTP | 305 头 + 42 体 | 347 字节 |
| 传输层 | TCP | 20 字节 | 367 字节 |
| 网络层 | IP | 20 字节 | 387 字节 |
| 链路层 | 回环帧 | 0 字节 | 387 字节 |

### 2.2 真实网络环境（非 localhost）

如果部署到远程服务器，数据包经过完整的网络栈：

```
应用层数据（347 字节 HTTP）
  ↓
TCP 层：添加 TCP 头部（20 字节）→ 367 字节 TCP 段
  ↓
IP 层：添加 IP 头部（20 字节）→ 387 字节 IP 数据报
  ↓
数据链路层（以太网）：添加 MAC 头部（14 字节）+ FCS 校验（4 字节）→ 405 字节以太网帧
  ↓
物理层：前导码（8 字节）+ 帧间距（12 字节）→ 425 字节物理层信号
  ↓
网卡：将数字信号转换为电信号 / 光信号，发送到网线 / 光纤
  ↓
经过交换机、路由器逐跳转发，最终到达目标服务器
```

**以太网帧结构（1518 字节 MTU）：**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       目标 MAC 地址（48 位）                    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       源 MAC 地址（48 位）                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      EtherType（16 位）       |                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               |
|                        IP 数据报（46~1500 字节）                |
|                                                               |
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    FCS 校验和（32 位）                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

---

## 阶段3：服务端接收 — 从网卡到 Tomcat 线程池

### 3.1 网卡 → 内核协议栈

```
网卡接收电信号 → DMA 直接写入内核内存中的 Ring Buffer
  ↓
网卡产生硬件中断（IRQ），通知 CPU 有新数据包到达
  ↓
CPU 执行中断处理程序（上半部），快速将数据包从 Ring Buffer 拷贝到 sk_buff
  ↓
下半部（软中断/ksoftirqd）执行 net_rx_action
  ↓
内核协议栈逐层处理：
  ┌─ 链路层：检查 MAC 地址，剥去以太网头部
  ├─ 网络层：检查 IP 头部，校验和验证，路由查找，剥去 IP 头部
  └─ 传输层：检查 TCP 头部，序列号/确认号处理，校验和验证，找到对应的 socket
  ↓
数据进入 socket 接收缓冲区（tcp_rcv_established）
  ↓
唤醒等待 epoll_wait() 的 Tomcat 工作线程
```

**字节级细节：**

| 组件 | 数据结构 | 大小 | 说明 |
|------|---------|------|------|
| DMA Ring Buffer | 环状队列 | 每项 2048 字节 | 每个描述符指向一个 sk_buff |
| sk_buff | 内核结构体 | ~240 字节 | 头部 + 数据指针，管理网络包 |
| socket 接收缓冲区 | 内核内存 | 默认 212992 字节（208 KB） | `/proc/sys/net/ipv4/tcp_rmem` |
| epoll 事件 | epoll_event | 12 字节 | 包含 fd + 事件类型 |

### 3.2 Tomcat 接收连接 — NIO 模型

```
Tomcat 的 Acceptor 线程在 ServerSocketChannel.accept() 上阻塞
  ↓
epoll_wait() 返回，有新的连接就绪
  ↓
Acceptor 接收连接，创建 NioChannel 对象
  ↓
将 NioChannel 注册到 Poller 线程的 Selector 上，关注 OP_READ 事件
  ↓
Poller 线程轮询 Selector，发现可读事件
  ↓
读取 socket 数据到 NioByteBuffer
  ↓
解析 HTTP 请求行和头部（org.apache.coyote.http11.Http11InputBuffer）
  ↓
封装为 org.apache.coyote.Request 对象
  ↓
提交到 Tomcat 线程池（Worker 线程池）
```

**Tomcat NIO 模型线程架构：**

```
┌─────────────────────────────────────────────────────────────┐
│                    Tomcat 线程架构                          │
│                                                             │
│  Acceptor 线程（1 个）                                      │
│    └─ 接收新连接，注册到 Poller                             │
│                                                             │
│  Poller 线程（2 个，可配置）                                 │
│    └─ Selector 轮询，读取请求数据                            │
│                                                             │
│  Worker 线程池（默认 200 个）                                │
│    └─ 执行 Servlet 容器请求处理                              │
│       └─ 调用 Spring DispatcherServlet                      │
└─────────────────────────────────────────────────────────────┘
```

**字节级细节：**

| 组件 | 大小 | 说明 |
|------|------|------|
| NioChannel | ~200 字节 | 封装 SocketChannel + 缓冲区 |
| Http11InputBuffer | ~512 字节 | 解析 HTTP 请求的内部缓冲区 |
| Tomcat 线程池栈 | 默认 1 MB | 每个工作线程的 JVM 栈 |
| NioByteBuffer | 默认 8192 字节（8 KB） | 读取 socket 数据的缓冲区 |

### 3.3 HTTP 请求解析

Tomcat 解析 HTTP 请求时，将原始字节流转换为 Java 对象：

```
原始字节（347 字节 HTTP 请求报文）
  ↓
Http11Processor 逐字节解析：
  ┌─ 读取请求行直到 \r\n
  ├─ 读取每个头部直到 \r\n
  ├─ 遇到空行 \r\n 后停止读取头部
  └─ 根据 Content-Length 读取请求体
  ↓
org.apache.catalina.connector.Request 对象
  ↓
包含：method, requestURI, headers[], body[]
```

---

## 阶段4：SpringBoot 核心处理链 — DispatcherServlet 全流程

### 4.1 Servlet 容器 → DispatcherServlet

```
Worker 线程执行：
  ↓
StandardWrapperValve.invoke() → 调用 Servlet.service()
  ↓
HttpServlet.service() 根据 method 分发到 doPost() / doGet()
  ↓
FrameworkServlet.doPost() → processRequest()
  ↓
DispatcherServlet.doDispatch()  ← 核心入口！
```

### 4.2 DispatcherServlet 核心处理链

```
doDispatch() 执行流程：
  ↓
① checkedMultipart → 检查是否为 multipart 请求（文件上传）
  ↓
② getHandler() → 遍历 HandlerMapping 链表：
   ┌─ RequestMappingHandlerMapping：根据 @RequestMapping 注解匹配
   │   └─ 查找路径 /api/auth/login 对应的 HandlerMethod
   └─ 返回 HandlerExecutionChain（HandlerMethod + InterceptorList）
  ↓
③ getHandlerAdapter() → 找到支持该 Handler 的适配器：
   └─ RequestMappingHandlerAdapter
  ↓
④ 执行拦截器 preHandle() 链
  ↓
⑤ handle() → 调用 HandlerMethod 的 invokeForRequest()
  ↓
⑥ 执行拦截器 postHandle() 链
  ↓
⑦ processDispatchResult() → 处理视图渲染或 @ResponseBody
```

**字节级细节：**

| 步骤 | 关键对象 | 大小/复杂度 | 说明 |
|------|---------|------------|------|
| ① Multipart 检查 | 布尔值 | 1 字节 | 快速判断 |
| ② HandlerMapping | HandlerMethod | ~200 字节 | 封装 Method 对象 + Bean 引用 |
| ③ HandlerAdapter | 适配器 | 单例 | 复用，无新增内存 |
| ④ 拦截器链 | 数组 | 每项 ~8 字节引用 | 本项目：JWT 拦截器 + 日志拦截器 |
| ⑤ 方法调用 | 反射调用 | 参数解析 | 见下方详细说明 |
| ⑥ 视图处理 | ModelAndView | 或 null | @ResponseBody 直接写响应 |

### 4.3 参数解析与数据绑定

```
HandlerMethod.invokeForRequest()
  ↓
InvocableHandlerMethod.getMethodArgumentValues()
  ↓
遍历所有方法参数，为每个参数找到对应的 HandlerMethodArgumentResolver
  ↓
以 @RequestBody UserLoginDTO 为例：
  ├─ RequestResponseBodyMethodProcessor 处理 @RequestBody
  ├─ 从 HttpInputMessage 读取 body 字节流
  ├─ 使用 Jackson ObjectMapper 反序列化 JSON
  └─ 将字节转换为 Java 对象
```

**@RequestBody 反序列化过程（字节级）：**

```
原始字节流（42 字节 JSON）：
{"username":"admin","password":"123456"}

Jackson 解析过程：
  ↓
JsonParser 逐字符扫描：
  {  → 开始对象
  "username" → 字段名
  "admin"    → 字符串值
  "password" → 字段名
  "123456"   → 字符串值
  }  → 结束对象
  ↓
ObjectMapper 通过反射 / MethodHandle 创建 UserLoginDTO 对象
  ↓
使用 setter 或直接字段赋值：
  dto.username = "admin"   (String 对象，~40 字节)
  dto.password = "123456"  (String 对象，~42 字节)
  ↓
UserLoginDTO 对象内存布局（HotSpot JVM）：
  ┌─ 对象头：Mark Word（8 字节）+ Klass Pointer（4 字节，压缩）= 12 字节
  ├─ 实例数据：
  │   ├─ username: String 引用（4 字节，压缩）
  │   └─ password: String 引用（4 字节，压缩）
  └─ 对齐填充：4 字节
  ─────────────────────────
  Total: 24 字节（对象本身）+ 2 个 String 对象
```

### 4.4 验证与安全

Spring Security 或自定义拦截器执行验证：

```
JWT 拦截器（preHandle）：
  ↓
从请求头提取 Authorization: Bearer <token>
  ↓
JwtUtil.validate(token)：
  ┌─ Base64 解码 Header（~30 字节）
  ├─ Base64 解码 Payload（~100 字节）
  ├─ HMAC-SHA256 签名验证（产生 32 字节摘要）
  └─ 比较签名是否一致
  ↓
JwtUtil.parseUsername(token)：
  └─ 从 Payload 中提取 sub 字段 → "admin"
  ↓
解析出用户信息，存入 SecurityContextHolder 或 request attribute
```

**JWT Token 结构（字节级）：**

```
Token: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwMDAwMDAwMH0.abc123...

Base64 解码后：

Header（~30 字节解码后）：
{"alg":"HS256"}

Payload（~50 字节解码后）：
{"sub":"admin","iat":1700000000}

Signature（32 字节）：
HMAC-SHA256 原始二进制摘要
```

### 4.5 控制器方法执行

以 `AuthController.login()` 为例：

```java
@PostMapping("/login")
public Result<?> login(@RequestBody @Valid UserLoginDTO dto) {
    // ① 参数校验：dto.username 和 dto.password 非空
    // ② 调用 UserService.authenticate(dto.username, dto.password)
    // ③ 生成 JWT Token
    // ④ 返回 Result.ok(token)
}
```

**字节级细节：**

| 操作 | 内存对象 | 大小 | 说明 |
|------|---------|------|------|
| 参数校验 | ConstraintViolation | 0（无违规） | 或 ~200 字节/条违规 |
| authenticate 参数 | String × 2 | ~80 字节 | "admin" + "123456" |
| JWT Token | String | ~180 字节 | Base64 编码后的 Token |
| 返回 Result | Result<String> | ~300 字节 | 包含 code, mes, data |

---

## 阶段5：业务逻辑层 — Service 与缓存/消息队列

### 5.1 Service 层调用链

```
AuthController.login()
  ↓
UserService.authenticate(username, password)
  ↓
① findUserableUser(username) → 查 Redis 缓存 → 未命中 → 查 MySQL
  ↓
② 比对密码（BCryptPasswordEncoder.matches(rawPwd, encodedPwd)）
  ↓
③ 生成 JWT Token
  ↓
④ 将用户信息写入 Redis 缓存（demo:user:active:{username}）
  ↓
返回 token
```

### 5.2 Cache-Aside 缓存模式（字节级）

```java
public User findUserableUser(String username) {
    String cacheKey = "demo:user:active:" + username;  // ~35 字节
    // 第 1 步：查 Redis
    User cached = (User) redisUtil.get(cacheKey);
    if (cached != null) return cached;  // 缓存命中，直接返回

    // 第 2 步：缓存未命中，查数据库
    User user = userMapper.findUseableUserByUsername(username);

    // 第 3 步：写入 Redis（10 分钟过期）
    if (user != null) {
        redisUtil.set(cacheKey, user);  // 序列化 User 对象
    }
    return user;
}
```

**字节级细节：**

| 操作 | 数据量 | 耗时（参考） | 说明 |
|------|--------|------------|------|
| Redis key 构造 | 35 字节 | ~0.1 μs | 字符串拼接 |
| Redis GET 命令 | ~50 字节 | ~0.3 ms（网络） | 发送 "GET key" 命令 |
| 反序列化 User | ~200 字节 JSON | ~0.5 ms | Jackson 反序列化 |
| 数据库查询 | ~500 字节 | ~5 ms | MySQL 查询返回行数据 |
| 序列化 User 到 Redis | ~200 字节 | ~0.5 ms | Jackson 序列化 |
| Redis SET 命令 | ~300 字节 | ~0.3 ms（网络） | 发送 "SET key" + 数据 |
| **总耗时（缓存命中）** | | **~0.8 ms** | |
| **总耗时（缓存未命中）** | | **~6.5 ms** | 包含数据库查询 |

### 5.3 Redis 序列化格式

本项目使用 `GenericJackson2JsonRedisSerializer`，Redis 中存储的 JSON 格式：

```json
{
  "@class": "com.xuwenye.demo.Entity.User",
  "id": 1,
  "username": "admin",
  "password": null,
  "nickname": "管理员",
  "avatar": null,
  "email": "admin@example.com",
  "status": 1,
  "userRole": "ROLE_ADMIN",
  "balance": 9999.99,
  "createTime": ["java.time.LocalDateTime", 1700000000],
  "updateTime": ["java.time.LocalDateTime", 1700000000],
  "lastLoginTime": ["java.time.LocalDateTime", 1700000000],
  "isDeleted": 0
}
```

**字节级细节：**

| 字段 | 字节数 | 说明 |
|------|--------|------|
| `@class` 类型信息 | ~40 | 存储全限定类名 |
| id | ~10 | `1` |
| username | ~20 | `"admin"` |
| nickname | ~30 | `"管理员"`（UTF-8，每个汉字 3 字节） |
| userRole | ~30 | `"ROLE_ADMIN"` |
| balance | ~15 | `9999.99` |
| createTime | ~50 | `["java.time.LocalDateTime", ...]` |
| 其他字段 | ~100 | 各种 null 值和短字段 |
| **总计** | **~300 字节** | 序列化后的 User 对象 |

### 5.4 密码校验（BCrypt）

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
boolean matches = encoder.matches(rawPassword, encodedPassword);
```

**字节级细节：**

| 操作 | 数据量 | 耗时 | 说明 |
|------|--------|------|------|
| rawPassword | 6 字节 | — | `"123456"` |
| encodedPassword | 60 字节 | — | 存储在数据库中的 BCrypt 哈希 |
| BCrypt 计算 | 60 字节 | ~10 ms | 暴力破解保护，故意慢 |
| 匹配结果 | 1 字节 | — | true/false |

### 5.5 分布式锁（Redisson Watchdog）

```java
RLock lock = redissonClient.getLock("demo:stock:lock:" + productId);
lock.lock(30, TimeUnit.SECONDS);  // Watchdog 自动续期
try {
    // 扣减库存
} finally {
    lock.unlock();
}
```

**字节级细节：**

| 操作 | 数据量 | 说明 |
|------|--------|------|
| Redis key | ~40 字节 | `demo:stock:lock:1` |
| Redisson Lock 数据结构 | 1 个 Redis Hash | 包含线程 ID + 重入计数 |
| Watchdog 周期 | 每 10 秒续期 | 自动续期直到 unlock |
| Lua 脚本执行 | ~200 字节 | 原子解锁操作 |

---

## 阶段6：数据持久层 — MyBatis → JDBC → MySQL 存储引擎

### 6.1 MyBatis 执行流程

```
UserService.findUseableUserByUsername("admin")
  ↓
UserMapper 接口代理对象（MyBatis-Plus 动态代理）
  ↓
MapperProxy.invoke()
  ↓
SqlSession.selectOne("findUseableUserByUsername", "admin")
  ↓
Executor 执行器（CachingExecutor → BaseExecutor）
  ↓
StatementHandler 创建 PreparedStatement
  ↓
ParameterHandler 设置参数
  ↓
TypeHandler 将 Java 参数转换为 JDBC 类型
  ↓
java.sql.PreparedStatement.executeQuery()
  ↓
ResultSetHandler 处理结果集
  ↓
TypeHandler 将 JDBC 类型转换为 Java 对象
  ↓
返回 User 对象
```

### 6.2 SQL 解析与参数绑定

```xml
<!-- UserMapper.xml -->
<select id="findUseableUserByUsername" resultType="com.xuwenye.demo.Entity.User">
    SELECT * FROM sys_user
    WHERE username = #{username}
      AND is_deleted = 0
      AND status = 1
</select>
```

**JDBC 通信过程（字节级）：**

```
PreparedStatement 设置参数：
  ↓
MySQL 二进制协议包（Text Protocol）：
  ┌─ 命令包头部：4 字节（3 字节包长度 + 1 字节序列号）
  ├─ 命令类型：1 字节（0x03 = COM_QUERY）
  └─ SQL 语句文本：~100 字节
  ─────────────────────────────────
  Total: ~105 字节

完整的 SQL 语句（~100 字节）：
SELECT * FROM sys_user WHERE username = 'admin' AND is_deleted = 0 AND status = 1

服务器返回结果集：
  ┌─ 结果集头部：包含列定义 ~200 字节
  ├─ 行数据：
  │   ├─ id: 1 (length-encoded integer, 1 字节)
  │   ├─ username: "admin" (length-encoded string, 7 字节)
  │   ├─ password: "$2a$10$..." (length-encoded string, 62 字节)
  │   ├─ nickname: "管理员" (length-encoded string, 7 字节)
  │   ├─ email: "admin@example.com" (length-encoded string, 20 字节)
  │   ├─ status: 1 (1 字节)
  │   ├─ user_role: "ROLE_ADMIN" (length-encoded string, 12 字节)
  │   ├─ balance: 9999.99 (length-encoded decimal, 8 字节)
  │   ├─ create_time: 2026-01-01 00:00:00 (12 字节)
  │   ├─ update_time: 2026-01-01 00:00:00 (12 字节)
  │   ├─ last_login_time: 2026-01-01 00:00:00 (12 字节)
  │   └─ is_deleted: 0 (1 字节)
  │   ─────────────────────────────────
  │   行数据总计: ~156 字节
  └─ EOF 包: 5 字节
  ─────────────────────────────────
  Total 响应: ~361 字节
```

### 6.3 MySQL 存储引擎内部

```
MySQL Server 接收到 SQL 查询请求
  ↓
SQL 解析器（Parser）→ 词法分析 → 语法分析 → 生成解析树（~500 字节临时对象）
  ↓
查询优化器（Optimizer）→ 选择最优执行计划
  ↓
打开 sys_user 表 → 检查表结构缓存（Table Definition Cache）
  ↓
存储引擎（InnoDB）执行：
  ┌─ 检查缓冲池（Buffer Pool）是否有该页
  ├─ 无 → 从磁盘加载
  └─ 有 → 直接返回
  ↓
使用索引（idx_username）查找 → B+ 树遍历
  ↓
定位到聚簇索引 → 读取行数据
  ↓
返回结果给 MySQL Server → 发送给客户端
```

**InnoDB 存储引擎字节级细节：**

| 组件 | 大小 | 说明 |
|------|------|------|
| 页（Page） | 16 KB（默认） | InnoDB 最小存储单元 |
| 行（Row） | ~200 字节 | 单行用户数据 |
| B+ 树节点 | 1 页 = 16 KB | 可存储约 80 个索引条目 |
| Buffer Pool | 128 MB（默认配置） | 内存缓存池 |
| 聚簇索引 | 主键 id 组织 | 数据即索引 |
| 二级索引（username） | 每项 ~30 字节 | 指向主键的引用 |

**磁盘 IO 过程（字节级）：**

```
查询缓存未命中 → 触发磁盘 IO
  ↓
操作系统页缓存（Page Cache）检查
  ↓
仍然未命中 → 触发磁盘读取
  ↓
磁盘控制器通过 DMA 将 16 KB 数据页读入内存
  ↓
数据从内核缓冲区拷贝到 MySQL Buffer Pool
  ↓
InnoDB 解析数据页，找到匹配的行数据
```

| 操作 | 数据量 | 耗时 | 说明 |
|------|--------|------|------|
| 磁盘寻道 | 0 字节 | ~10 ms | 机械硬盘最慢部分 |
| 读取数据页 | 16 KB | ~0.5 ms (SSD) | 现代 SSD 随机读取 |
| 页内二分查找 | 16 KB | ~0.01 ms | CPU 处理 |
| 数据拷贝到 MySQL | 200 字节 | ~0.001 ms | 从 Buffer Pool 到应用 |

### 6.4 MyBatis 结果集映射（字节级）

```
ResultSet 一行数据（~156 字节原始二进制）
  ↓
TypeHandler 逐字段转换：
  ┌─ id: LongTypeHandler → Long (8 字节)
  ├─ username: StringTypeHandler → String (~40 字节)
  ├─ password: StringTypeHandler → String (60 字节，立即置 null 脱敏)
  ├─ nickname: StringTypeHandler → String (~30 字节)
  ├─ email: StringTypeHandler → String (20 字节)
  ├─ status: IntegerTypeHandler → Integer (16 字节)
  ├─ user_role: StringTypeHandler → String (20 字节)
  ├─ balance: BigDecimalTypeHandler → BigDecimal (~40 字节)
  ├─ create_time: LocalDateTimeTypeHandler → LocalDateTime (24 字节)
  ├─ update_time: LocalDateTimeTypeHandler → LocalDateTime (24 字节)
  ├─ last_login_time: LocalDateTimeTypeHandler → LocalDateTime (24 字节)
  └─ is_deleted: IntegerTypeHandler → Integer (16 字节)
  ↓
反射创建 User 对象（通过 MyBatis 反射工厂或直接构造）
  ↓
通过 setter 或直接字段赋值填充 User 对象
  ↓
User 对象内存布局（JVM）：
  ┌─ 对象头：12 字节（压缩 OOPs）
  ├─ id: 8 字节 (long)
  ├─ username: 4 字节 (引用)
  ├─ password: 4 字节 (引用，置 null)
  ├─ nickname: 4 字节 (引用)
  ├─ avatar: 4 字节 (引用)
  ├─ email: 4 字节 (引用)
  ├─ status: 16 字节 (Integer 对象)
  ├─ userRole: 4 字节 (引用)
  ├─ balance: 4 字节 (引用)
  ├─ createTime: 4 字节 (引用)
  ├─ updateTime: 4 字节 (引用)
  ├─ lastLoginTime: 4 字节 (引用)
  └─ isDeleted: 16 字节 (Integer 对象)
  ─────────────────────────────────
  User 对象本身: ~88 字节
  引用的对象: ~200 字节
  Total: ~288 字节
```

---

## 阶段7：响应返回 — 序列化到浏览器渲染

### 7.1 响应序列化

```
Controller 方法返回 Result.ok(token)
  ↓
@ResponseBody 注解触发 RequestResponseBodyMethodProcessor
  ↓
HandlerMethodReturnValueHandler.handleReturnValue()
  ↓
AbstractMessageConverterMethodProcessor.writeWithMessageConverters()
  ↓
遍历 HttpMessageConverter 列表，找到支持 application/json 的转换器
  ↓
MappingJackson2HttpMessageConverter 处理
  ↓
ObjectMapper.writeValueAsString(result) 序列化为 JSON
```

**Result 对象序列化（字节级）：**

```java
public class Result<T> {
    private int code;         // 200
    private String mes;       // null
    private T data;           // "eyJhbGciOiJIUzI1NiJ9..."
}
```

**序列化后的 JSON 响应：**

```json
{"code":200,"mes":null,"data":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwMDAwMDAwMH0.abc123..."}
```

**字节级分项：**

| 字段 | 字节数 | 说明 |
|------|--------|------|
| `{"code":200,` | 12 | 固定格式 |
| `"mes":null,` | 12 | 固定格式 |
| `"data":"` | 8 | 固定格式 |
| JWT Token | ~180 | Base64 编码 |
| `"}` | 2 | 结尾 |
| **总计** | **~214 字节** | HTTP 响应体 |

### 7.2 HTTP 响应报文

```
HTTP/1.1 200 OK\r\n                                           ← 状态行 ~18 字节
Content-Type: application/json\r\n                             ← 头部 ~30 字节
Content-Length: 214\r\n                                        ← 头部 ~20 字节
Set-Cookie: ...\r\n                                            ← 可选，~100 字节
\r\n                                                             ← 空行 2 字节
{"code":200,"mes":null,"data":"eyJhbGciOiJIUzI1NiJ9..."}      ← 响应体 ~214 字节
```

**字节级总计：**

| 响应部分 | 字节数 |
|----------|--------|
| 状态行 | 18 字节 |
| 响应头 | ~50 字节 |
| 空行 | 2 字节 |
| 响应体 | 214 字节 |
| **总计** | **~284 字节** |

### 7.3 响应返回客户端

```
Tomcat 工作线程将响应字节写入 NioChannel 缓冲区
  ↓
Poller 线程检测到可写事件
  ↓
内核 TCP 栈将数据封装为 TCP 段
  ↓
loopback 接口将数据拷贝到客户端接收缓冲区
  ↓
浏览器（Chromium 网络栈）收到响应
  ↓
V8 引擎解析 JSON 响应
  ↓
Axios 库处理 Promise resolve
  ↓
Vue 组件更新响应式数据
  ↓
Vue 虚拟 DOM diff → 更新真实 DOM
  ↓
浏览器渲染引擎（Blink）布局 → 绘制 → 合成 → 显示在屏幕上
```

### 7.4 浏览器渲染过程（字节级）

```
Vue 响应式数据变更
  ↓
触发组件重新渲染（render 函数执行）
  ↓
创建新的 VNode 树（虚拟 DOM 节点，每个 ~40 字节）
  ↓
Diff 算法比较新旧 VNode 树
  ↓
Patch 操作 → 更新真实 DOM
  ↓
Blink 渲染引擎：
  ┌─ Style Recalc：重新计算 CSS 样式（~0.5 ms）
  ├─ Layout：计算元素位置和大小（~1 ms）
  ├─ Paint：生成绘制指令（~0.5 ms）
  └─ Composite：合成图层，显示在屏幕上（~0.3 ms）
  ↓
屏幕显示 → 用户看到结果
```

**VNode 对象内存布局（字节级）：**

```
VNode 对象（~40 字节，HotSpot 压缩 OOPs）：
  ┌─ 对象头：12 字节
  ├─ tag: 4 字节 (String 引用，如 "div")
  ├─ data: 4 字节 (Object 引用，属性/事件等)
  ├─ children: 4 字节 (Array 引用，子节点)
  ├─ text: 4 字节 (String 引用，文本内容)
  ├─ elm: 4 字节 (Object 引用，对应真实 DOM 节点)
  ├─ key: 4 字节 (String 引用，diff 优化用)
  └─ flags: 4 字节 (int，节点类型标记)
```

---

## 阶段8：异步处理 — ActiveMQ 消息与 Redis 缓存刷新

### 8.1 ActiveMQ 消息队列架构

本项目使用 ActiveMQ（Classic）作为异步消息队列，处理订单状态变更和缓存刷新：

```
业务线程（生产者）                    ActiveMQ Broker              消费者线程
    |                                    |                           |
    |---- send(textMessage) ------------>|                           |
    |                                    |-- 持久化到 KahaDB ------->|
    |                                    |                           |
    |                                    |---- 推送给消费者 --------->|
    |                                    |                           |-- 处理消息
    |                                    |                           |-- 更新数据库
    |                                    |                           |-- 清除缓存
```

### 8.2 消息生产者（MQProducer）

```java
@Component
public class MQProducer {
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    // 发送缓存刷新任务
    public void sendCacheRefreshTask(String module, String action, List<String> keys) {
        JSONObject message = new JSONObject();
        message.put("module", module);      // "product" / "user" / "seller" / "order"
        message.put("action", action);      // "clear" / "delete" / "refresh"
        message.put("keys", keys);          // 要清除的 Redis key 列表
        message.put("timestamp", System.currentTimeMillis());
        // 发送到 cache.queue
        jmsMessagingTemplate.convertAndSend("cache.queue", message.toJSONString());
    }

    // 发送订单任务
    public void sendOrderTask(Long orderId, String action) {
        JSONObject message = new JSONObject();
        message.put("orderId", orderId);
        message.put("action", action);      // "PAY" / "CANCEL" / "REFUND" / "COMPLETE"
        message.put("timestamp", System.currentTimeMillis());
        // 发送到 order.queue
        jmsMessagingTemplate.convertAndSend("order.queue", message.toJSONString());
    }
}
```

**消息字节级细节：**

| 消息类型 | 字段 | 字节数 | 说明 |
|---------|------|--------|------|
| 缓存刷新 | module | ~15 | `"product"` |
| | action | ~10 | `"clear"` |
| | keys 数组 | ~100 | 1~3 个 Redis key |
| | timestamp | ~15 | 时间戳 |
| | **总计** | **~140 字节** | JSON 序列化后 |
| 订单任务 | orderId | ~10 | `12345` |
| | action | ~10 | `"PAY"` |
| | timestamp | ~15 | 时间戳 |
| | **总计** | **~60 字节** | JSON 序列化后 |

### 8.3 消息消费者（MQConsumer）

```java
@JmsListener(destination = "cache.queue")
public void handleCacheRefresh(String message) {
    // 1. 解析 JSON 消息
    // 2. 获取 module/action/keys
    // 3. 遍历 keys，调用 redisUtil.del(key)
    // 4. 如果 action 是 "clear"，清除整个模块缓存
}

@JmsListener(destination = "order.queue")
public void handleOrder(String message) {
    // 1. 解析 JSON 消息
    // 2. 获取 orderId/action
    // 3. 根据 action 执行对应操作
    // 4. 更新订单状态
    // 5. 发送通知
}
```

**ActiveMQ 消息协议（OpenWire）字节级细节：**

| 协议字段 | 字节数 | 说明 |
|---------|--------|------|
| 消息头部 | ~50 字节 | 包含消息 ID、时间戳、持久化标记 |
| 消息属性 | ~30 字节 | JMS 属性（如队列名、优先级） |
| 消息体 | ~140 字节 | 实际业务数据（JSON 字符串） |
| **总计（每条消息）** | **~220 字节** | 网络上传输的完整消息 |

### 8.4 异步缓存刷新流程

以商品更新为例：

```
① 业务线程：ProductService.updateProduct()
   ├─ 更新数据库（MySQL）
   └─ 发送 MQ 消息：mqProducer.sendCacheRefreshTask("product", "clear", keys)
      └─ 消息体：~140 字节 JSON → ActiveMQ cache.queue

② 业务线程立即返回，不阻塞等待缓存清除

③ ActiveMQ Broker 收到消息：
   ├─ 持久化到 KahaDB（本地文件）
   └─ 推送给消费者线程

④ 消费者线程：MQConsumer.handleCacheRefresh()
   ├─ 解析消息（~140 字节 JSON）
   ├─ redisUtil.del("demo:product:page:1:10")    → 16 字节命令
   ├─ redisUtil.del("demo:product:detail:1")      → 16 字节命令
   └─ redisUtil.del("demo:product:category:all")  → 16 字节命令

⑤ 缓存清除完成
```

**同步 vs 异步对比：**

| 方式 | 业务线程阻塞时间 | 用户体验 | 数据库一致性 |
|------|----------------|---------|------------|
| 同步清除缓存 | ~3 ms（网络延迟） | 响应慢 3 ms | 最终一致 |
| 异步 MQ 清除 | ~0.3 ms（发送消息） | 几乎无感 | 最终一致 |
| **收益** | **快 10 倍** | **更好** | **相同** |

---

## 附录：字节级数据大小参考

### JVM 对象内存布局

| 类型 | 大小（压缩 OOPs） | 大小（未压缩） | 说明 |
|------|------------------|---------------|------|
| 对象头 | 12 字节 | 16 字节 | Mark Word (8) + Klass Pointer (4/8) |
| 引用 | 4 字节 | 8 字节 | 压缩后节省一半 |
| int | 4 字节 | 4 字节 | 值类型 |
| long | 8 字节 | 8 字节 | 值类型 |
| boolean | 1 字节 | 1 字节 | 值类型 |
| Integer | 16 字节 | 24 字节 | 对象头 + 4 字节 int + 对齐 |
| Long | 24 字节 | 24 字节 | 对象头 + 8 字节 long |
| String (空) | 24 字节 | 32 字节 | 对象头 + char[] 引用 + hash |
| String ("admin") | 24 + 16 = 40 字节 | 32 + 32 = 64 字节 | 含 char[] 数组 |
| BigDecimal | ~40 字节 | ~56 字节 | 含 BigInteger + scale |
| LocalDateTime | 24 字节 | 32 字节 | 含 date + time 两个 long |
| ArrayList (空) | 40 字节 | 48 字节 | 含 Object[] 引用 |
| HashMap (空) | 48 字节 | 64 字节 | 含 table, size, threshold 等 |

### 网络传输各层头部

| 协议层 | 头部大小 | 说明 |
|--------|---------|------|
| 以太网帧 | 14 字节 | MAC 目标（6）+ 源（6）+ Type（2） |
| IPv4 | 20 字节 | 标准头部（无选项） |
| TCP | 20 字节 | 标准头部（无选项） |
| HTTP | 无固定头部 | 可变长度，文本协议 |
| **链路层总开销** | **54 字节** | 以太网 + IP + TCP |

### 本项目关键数据大小速查表

| 数据对象 | 存储大小 | 网络传输大小 | 说明 |
|---------|---------|------------|------|
| HTTP 请求（登录） | ~347 字节 | ~387 字节（+TCP/IP 头） | 含 42 字节 JSON 体 |
| HTTP 响应（登录成功） | ~284 字节 | ~324 字节 | 含 214 字节 JSON 体 |
| JWT Token | ~180 字节 | 180 字节 | Base64 编码 |
| User 对象（Redis 序列化） | ~300 字节 | ~300 字节 | JSON 格式 |
| User 对象（JVM 内存） | ~288 字节 | — | Java 对象堆内存 |
| 用户数据库行 | ~200 字节 | ~361 字节（含协议头） | MySQL 行数据 |
| InnoDB 数据页 | 16 KB | 16 KB | 磁盘 IO 最小单元 |
| ActiveMQ 消息（缓存刷新） | ~220 字节 | ~220 字节 | OpenWire 协议 |
| TCP 三次握手 | 0 字节 | 120 字节（3 × 40） | 不含应用数据 |
| Redis 命令（GET） | ~50 字节 | ~50 字节 | Redis 协议文本 |

### 全链路数据总量

以用户登录为例，从键盘输入到屏幕显示的总数据量：

```
键盘输入 → 操作系统 → 浏览器 → HTTP 请求 → 服务器 → 数据库 → 响应 → 浏览器渲染

发送数据：~450 字节（HTTP 请求 + TCP 三次握手）
接收数据：~400 字节（HTTP 响应 + TCP 确认）
内部处理：~2 KB 临时对象（JVM 堆内存分配）
数据库 IO：16 KB（InnoDB 数据页）
Redis IO：~400 字节（缓存读写）

总计：~20 KB 数据流转（含磁盘 IO）
```

---

> 本文档以 ZuiMShop 项目为例，扫描了从字节级到应用层的全链路数据流。每个环节都涉及操作系统的进程调度、内存管理、文件系统、网络协议栈、JVM 运行时、数据库存储引擎等多个底层系统的协作，理解这些链路有助于定位性能瓶颈和排查数据异常。