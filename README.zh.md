# DataSync

**[English](README.md) | [中文](README.zh.md)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-39-47848f.svg)](https://www.electronjs.org/)

一个基于内容定义分块（CDC）算法的分布式文件同步与备份系统，支持增量传输、端到端加密、群组共享，并提供跨平台的 Electron 桌面客户端。

---

## 效果截图

| 首页 | 文件浏览器 |
|------|----------|
| ![首页](docs/screenshots/dashboard.png) | ![文件浏览器](docs/screenshots/file-explorer-grid.png) |

| 群组管理 | 群组文件浏览 |
|---------|------------|
| ![群组管理](docs/screenshots/group-management.png) | ![群组文件浏览](docs/screenshots/group-explorer.png) |

| 操作日志 | 同步算法选择 |
|---------|------------|
| ![日志](docs/screenshots/log-page.png) | ![同步算法](docs/screenshots/sync-algorithm.png) |

<details>
<summary>更多截图</summary>

| 登录 | 注册 |
|------|------|
| ![登录](docs/screenshots/login.png) | ![注册](docs/screenshots/register.png) |

| 文件列表视图 | 账号设置 |
|-----------|---------|
| ![文件列表](docs/screenshots/file-explorer-list.png) | ![账号设置](docs/screenshots/account-settings.png) |

</details>

---

## 目录

- [项目简介](#项目简介)
- [架构概览](#架构概览)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [配置参数说明](#配置参数说明)
- [功能说明](#功能说明)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [安全机制](#安全机制)
- [贡献指南](#贡献指南)
- [开源协议](#开源协议)

---

## 项目简介

DataSync 由三个子模块组成：

| 模块       | 描述                                              | 默认端口 |
| ---------- | ------------------------------------------------- | -------- |
| `server`   | Spring Boot 后端，存储同步文件、提供 REST 接口   | 8090     |
| `client-app` | Spring Boot 客户端代理，管理本地文件元数据     | 8092     |
| `sync-app` | Vue 3 + Electron 桌面 UI                         | —        |

**核心特性**

- **CDC 增量同步**：支持 FastCDC、FlipCDC、QuickCDC、RabinCDC 四种算法，仅传输变更块
- **双向同步**：上行（本地 → 服务器）与下行（服务器 → 本地，直接覆盖）
- **加密传输**：AES-256-GCM 数据加密 + RSA-2048-OAEP 密钥交换
- **Netty 高性能传输**：基于 Netty 的异步 Socket 通信
- **本地元数据**：客户端使用 SQLite 管理文件树及同步状态
- **定时同步**：支持 Cron 表达式配置自动同步策略
- **群组共享**：通过邮箱添加成员，将同步任务文件夹共享给群组，成员可浏览及下行同步
- **多用户隔离存储**：服务端以 `email/folderName` 路径命名空间区分用户目录
- **共享删除保护**：任务文件夹共享至有成员的群组时，禁止删除对应同步任务

---

## 架构概览

```
┌──────────────────────────────────────────────────────────────┐
│                sync-app (Electron + Vue 3)                    │
│  Dashboard / FileExplorer / GroupPage / GroupExplorer         │
│  Login / Register                                             │
└───────────────────────┬──────────────────────────────────────┘
                        │ HTTP (Axios, port 8092)
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                 client-app (Spring Boot)                      │
│  SQLite │ FileService │ SyncController │ GroupController      │
│  NettyClientManager → NettySyncClient                        │
└──────┬──────────────────────────────────────┬────────────────┘
       │ REST (HTTP, port 8090)               │ Netty (port 8443)
       ▼                                      ▼
┌──────────────────────────────────────────────────────────────┐
│                   server (Spring Boot)                        │
│  MySQL │ FileService │ ServerSyncController │ GroupController │
│  NettySyncServer ← SyncServerHandler                         │
└──────────────────────────────────────────────────────────────┘
```

详细架构说明见 [ARCHITECTURE.md](./ARCHITECTURE.md)。

**上行同步流程**

1. 前端触发 `POST /client/sync/upload`
2. Client 使用选定 CDC 算法对本地文件分块，计算 SHA-256 哈希
3. Client 调用 `POST /server/file/compare`，Server 返回差量块列表
4. Client 通过 Netty 将差量块（AES-GCM 加密）传送至 Server
5. Server 重建文件（临时文件 → 原子重命名），删除已不存在的文件
6. Client 更新本地 SQLite `is_sync = true`

**下行同步流程**

1. 前端触发 `POST /client/sync/download`
2. Client 调用 `POST /server/file/download`，Server 扫描对应范围目录
3. Server 将全部文件以 Base64 编码返回
4. Client 逐文件写入本地（直接覆盖）
5. Client 更新本地 SQLite `is_sync = true`

---

## 技术栈

### 后端（server & client-app）

| 技术            | 版本     | 用途                  |
| --------------- | -------- | --------------------- |
| Java            | 21       | 开发语言              |
| Spring Boot     | 3.4.2    | Web 框架              |
| MyBatis         | 3.0.4    | ORM / SQL 映射        |
| Netty           | —        | 高性能异步 Socket     |
| JJWT            | 0.12.5   | JWT 鉴权              |
| MySQL           | 8.0+     | Server 持久化存储     |
| SQLite          | —        | Client 本地元数据     |
| Redis           | 6+       | Server 缓存           |
| Lombok          | —        | 样板代码生成          |

### 前端（sync-app）

| 技术         | 版本 | 用途                        |
| ------------ | ---- | --------------------------- |
| Vue 3        | 3.5  | 前端框架（Composition API） |
| Vue Router   | 4    | 客户端路由                  |
| Electron     | 39   | 桌面应用容器                |
| Tailwind CSS | 3    | 原子化 CSS 框架             |
| Axios        | —    | HTTP 客户端                 |

---

## 目录结构

```
datasync/
├── server/                          # Spring Boot 服务端
│   └── src/main/java/
│       ├── backend/
│       │   ├── controller/          # REST 接口（鉴权、同步、群组、用户）
│       │   ├── service/             # 业务逻辑
│       │   ├── model/               # 实体类与 DTO
│       │   ├── sync/                # Netty 服务端
│       │   └── util/
│       └── dataSync/                # CDC 算法实现
│           ├── FastCDC/
│           ├── FlipCDC/
│           ├── QuickCDC/
│           └── RabinCDC/
│
├── client-app/                      # Spring Boot 客户端代理
│   └── src/main/java/
│       ├── backend/
│       │   ├── controller/          # REST 接口（文件、同步、群组、用户、日志）
│       │   ├── service/             # 业务逻辑 + 文件树管理
│       │   ├── mapper/sqlite/       # MyBatis SQLite 映射
│       │   ├── sync/                # Netty 客户端
│       │   └── task/                # 定时同步 + 文件监听
│       └── dataSync/                # CDC 算法实现（与 server 相同）
│
├── sync-app/                        # Electron + Vue 3 桌面端
│   └── src/renderer/src/
│       ├── views/                   # Dashboard、FileExplorer、GroupPage 等
│       ├── components/
│       └── utils/request.js         # Axios 封装
│
├── docs/screenshots/                # UI 截图
├── API.md                           # 接口文档
├── Database Tables.md               # 数据库表结构文档
└── ARCHITECTURE.md                  # 系统架构详解
```

---

## 快速开始

### 前置要求

| 依赖      | 版本   | 说明                         |
|-----------|--------|------------------------------|
| Java JDK  | 21+    | server 和 client-app 必须    |
| Maven     | 3.8+   | 或使用内置 `mvnw` wrapper    |
| Node.js   | 18+    | sync-app 必须                |
| MySQL     | 8.0+   | 服务端持久化存储             |
| Redis     | 6+     | 服务端缓存                   |
| OpenSSL   | 任意   | 生成 RSA 密钥对              |

---

### 1. 克隆仓库

```bash
git clone https://github.com/Alexander-Bruce/datasync.git
cd datasync
```

---

### 2. 配置服务端

**创建 MySQL 数据库：**

```sql
CREATE DATABASE datasync CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**复制配置模板并填写：**

```bash
cp server/src/main/resources/application-dev.yml.example \
   server/src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`，参见[配置参数说明](#配置参数说明)。

**生成 RSA 密钥对（Netty 加密传输用）：**

```bash
mkdir -p server/conf
openssl genrsa -out server/conf/rsa-private.pem 2048
openssl rsa -in server/conf/rsa-private.pem -pubout -out server/conf/rsa-public.pem
```

**启动服务端：**

```bash
cd server
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

服务端监听 **8090 端口**（HTTP），**8443 端口**（Netty，可配置）。

---

### 3. 配置客户端代理

```bash
cp client-app/src/main/resources/application-dev.yml.example \
   client-app/src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`，并复制服务端公钥：

```bash
mkdir -p client-app/conf
cp server/conf/rsa-public.pem client-app/conf/rsa-public.pem
```

**启动客户端代理：**

```bash
cd client-app
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

客户端代理监听 **8092 端口**。

---

### 4. 启动桌面客户端

```bash
cd sync-app
npm install
npm run dev        # 开发模式（带热重载）
npm run build      # 打包发布版本
```

---

## 配置参数说明

### 服务端（`server/src/main/resources/application-dev.yml`）

```yaml
application:
  datasource:
    mysql:
      url: jdbc:mysql://<host>:3306/datasync   # MySQL 连接地址
      username: <mysql用户名>
      password: <mysql密码>

  redis:
    master:
      host: <redis主节点地址>
      port: 6379
      password: <redis密码>
    slave:
      host: <redis从节点地址>      # 单节点部署可与 master 相同
      port: 6380
      password: <redis密码>

  aws:
    s3:
      accesskey: <S3访问密钥>      # 可选，使用 S3 存储后端时才需要
      secretkey: <S3私钥>
      region: auto
      endpoint: <S3兼容端点URL>
      bucket: <存储桶名称>
      path-style-access: true

  netty:
    server:
      port: 8443                        # Netty 监听端口（需与客户端一致）
      basePath: /path/to/server/storage # 服务端文件存储根目录

  jwt:
    secretkey: <Base64编码的256位密钥>  # 生成命令：openssl rand -base64 32
    freshTokenExpiration: 2592000000    # Refresh Token 有效期（毫秒），默认30天
    accessTokenExpiration: 25920000000  # Access Token 有效期（毫秒），默认300天
    token-name: Authorization
    uid: uid
    role: role

  mybatis:
    type-aliases-package: backend.model.entity
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 客户端代理（`client-app/src/main/resources/application-dev.yml`）

```yaml
application:
  datasource:
    sqlite:
      url: jdbc:sqlite:datasync-user.db?journal_mode=WAL&busy_timeout=5000

  netty:
    server:
      port: 8443                          # 必须与服务端 netty.server.port 一致
      basePath: /path/to/local/sync/root  # 本地同步文件根目录
    client:
      port: 8443                          # 必须与服务端 netty.server.port 一致
      host: <服务端IP或域名>

  jwt:
    secretkey: <与服务端相同的密钥>       # 必须与服务端 JWT secret 完全一致
    freshTokenExpiration: 2592000000
    accessTokenExpiration: 25920000000
    token-name: Authorization
    uid: uid
    role: role

  mybatis:
    type-aliases-package: backend.model.entity
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

> **提示：** 生成 JWT 密钥：`openssl rand -base64 32`

---

## 功能说明

### 同步任务管理（Dashboard）

- **新建任务**：选择本地路径、CDC 算法、远端主机、定时策略
- **修改任务**：更新任何配置项；路径变化时自动重新扫描文件树
- **删除任务**：同时清除本地 SQLite 中的 File + SubFile 记录；若已共享至有成员的群组，则拒绝删除
- **上行同步**：CDC 分块 → 差量比对 → Netty 增量传输
- **下行同步**：拉取服务端全量文件 → 本地覆盖

### 文件浏览器（FileExplorer）

- 分级浏览文件夹，支持面包屑导航
- 网格 / 列表双视图切换
- 右键上下文菜单：打开、上行同步、下行同步、删除
- 实时展示各文件同步状态

### 群组管理（GroupPage）

- 创建 / 删除群组
- 通过邮箱添加 / 移除成员
- 为群组添加 / 移除共享文件夹
- 成员标签区分群组创建者与普通成员

### 群组文件浏览器（GroupExplorer）

- 路径导航：双击文件夹进入，面包屑一键跳转
- 网格 / 列表双视图切换
- 下行同步按钮：异步将当前 scope 下载到指定本地路径

---

## API 文档

详见 [API.md](./API.md)

---

## 数据库设计

详见 [Database Tables.md](./Database%20Tables.md)

---

## 安全机制

| 机制           | 实现                                             |
| -------------- | ------------------------------------------------ |
| 身份认证       | JWT（Access Token + Refresh Token）              |
| 密码存储       | BCrypt（强度 12）                                |
| 传输加密       | AES-256-GCM（每个数据包独立 IV）                |
| 密钥交换       | RSA-2048 OAEP-SHA256                             |
| 跨域控制       | Spring Security CORS 配置                        |
| 会话策略       | Stateless（无服务端 Session）                    |

> **注意**：请勿将 `application-dev.yml` 或 `application-prod.yml` 提交到版本控制——这两个文件包含敏感凭据，已在 `.gitignore` 中排除。请使用仓库提供的 `.example` 文件作为配置模板。

---

## 贡献指南

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/my-feature`
3. 提交更改：`git commit -m 'feat: add my feature'`
4. 推送分支：`git push origin feature/my-feature`
5. 创建 Pull Request

Java 代码请遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)，前端代码提交前请运行 `prettier`。

---

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
