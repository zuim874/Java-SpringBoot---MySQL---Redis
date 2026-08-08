# nginx 反向代理 + 限流配置

## 架构

```
浏览器/前端(vite 5173)
    │  /api/...  /uploads/...
    ▼
nginx (8081)
    ├── /api/      → 转发 Spring Boot (127.0.0.1:8080) + 限流
    ├── /uploads/  → 转发 Spring Boot (127.0.0.1:8080) 静态映射（头像等上传文件）
    └── /          → 静态文件（前端打包产物 html 目录）
```

## 安装步骤

安装 nginx：https://nginx.org/en/download.html

将本项目 `nginx/` 下的两个文件复制到 nginx 安装目录：

| 项目文件 | nginx 目标 | 说明 |
|---|---|---|
| `nginx/nginx.conf` | `conf/nginx.conf` | 完整配置（含 /api/、/uploads/ 转发与限流） |
| `nginx/api_limit.conf` | `conf/api_limit.conf` | 精简版：仅 /api/auth/ 限流（可选的补充片段） |

> 确保 Spring Boot 运行在 **8080** 端口。

## 启动 / 重载

```bash
# cmd（在 nginx 安装目录下）
nginx -t
# 确保输出：
#   nginx: the configuration file D:\nginx-1.30.4/conf/nginx.conf syntax is ok
#   nginx: configuration file D:\nginx-1.30.4/conf/nginx.conf test is successful

nginx -s reload   # 修改配置后重载
nginx             # 启动
```

此时多次并行请求 `/api/` 会触发 nginx 限流（rate=5r/m，burst=2，超限返回 444），实现接口限流。

## 头像等上传文件说明

- 上传文件保存在 Spring Boot 项目根目录 `uploads/avatars/` 下
- 浏览器访问 `/uploads/avatars/xxx.jpg` → nginx `/uploads/` → Spring Boot 静态映射（WebMvcConfig）→ 本地文件
- **前端 vite 代理**：`frontend/vite.config.js` 已将 `/api` 与 `/uploads` 都代理到 `http://localhost:8081`（nginx）
