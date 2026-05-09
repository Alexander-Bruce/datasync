# DataSync

**[English](README.md) | [中文](README.zh.md)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-39-47848f.svg)](https://www.electronjs.org/)

一个基于内容定义分块（CDC）算法的分布式文件同步与备份系统，支持增量传输、端到端加密，并提供跨平台的 Electron 桌面客户端。

---

## 目录

- [项目简介](#项目简介)
- [架构概览](#架构概览)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [功能说明](#功能说明)
- [API 文档](#api-文档)
- [数据库设计](#数据库设计)
- [安全机制](#安全机制)

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
- **多用户隔离存储**：服务端以 `email/folderName` 路径命名空间区分每位用户的同步目录，彻底避免不同用户同名文件夹相互覆盖
- **共享删除保护**：任务文件夹已共享至有成员的群组时，禁止删除对应同步任务；须先删除群组或移除所有成员

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
       │ REST (HTTP, port 8090)               │ Netty (port 8888)
       ▼                                      ▼
┌──────────────────────────────────────────────────────────────┐
│                   server (Spring Boot)                        │
│  MySQL │ FileService │ ServerSyncController │ GroupController │
│  GroupService → groups.json                                   │
│  NettySyncServer ← SyncServerHandler                         │
└──────────────────────────────────────────────────────────────┘
```

**上行同步流程**

1. 前端触发 `POST /client/sync/upload`
2. Client 使用选定的 CDC 算法对本地文件分块，计算 SHA-256 哈希
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
| MySQL           | —        | Server 持久化存储     |
| SQLite          | —        | Client 本地元数据     |
| Redis           | —        | Server 缓存           |
| Lombok          | —        | 样板代码生成          |

### 前端（sync-app）

| 技术        | 版本 | 用途               |
| ----------- | ---- | ------------------ |
| Vue 3       | —    | 前端框架（Composition API） |
| Vue Router  | 4    | 客户端路由         |
| Electron    | —    | 桌面应用容器       |
| Axios       | —    | HTTP 客户端        |

---

## 目录结构

```
datasync-app/
├── server/                          # Spring Boot 服务端
│   └── src/main/java/backend/
│       ├── controller/
│       │   ├── UnAuthController.java        # 登录 / 注册（无鉴权）
│       │   ├── GroupController.java         # 群组管理接口
│       │   ├── client/SyncController.java   # 上行同步触发接口
│       │   └── server/ServerSyncController.java # 比对 & 下行接口
│       ├── service/
│       │   ├── FileService.java
│       │   ├── GroupService.java
│       │   └── impl/
│       │       ├── FileServiceImpl.java
│       │       └── GroupServiceImpl.java    # groups.json 读写 + 文件树遍历
│       ├── model/
│       │   ├── entity/User.java
│       │   ├── Group.java                   # 群组实体（存入 groups.json）
│       │   ├── GroupInfo.java               # 群组文件结构响应 DTO
│       │   ├── GroupScopeInfo.java          # 单个 scope 文件列表 DTO
│       │   └── GroupFileNode.java           # 文件节点 DTO（name/relativePath/dir）
│       ├── sync/                    # Netty 服务端
│       │   ├── server/NettySyncServer.java
│       │   └── server/SyncServerHandler.java
│       └── util/
│           ├── SyncStyle.java
│           └── DownloadFileInfo.java        # 下行同步 DTO
│
├── client-app/                      # Spring Boot 客户端代理
│   └── src/main/java/backend/
│       ├── controller/
│       │   ├── FileController.java          # 文件列表接口
│       │   ├── GroupController.java         # 群组代理接口（转发 + 异步下载）
│       │   ├── SyncController.java          # 上行 / 下行 / 任务管理
│       │   └── UserController.java          # 用户登录注册
│       ├── service/
│       │   ├── FileService.java             # 新增 downloadScope()
│       │   └── impl/FileServiceImpl.java    # 核心业务逻辑
│       ├── mapper/sqlite/
│       │   ├── FileMapper.java
│       │   └── SubFileMapper.java
│       ├── model/entity/
│       │   ├── File.java
│       │   └── SubFile.java
│       ├── sync/                    # Netty 客户端
│       │   ├── NettyClientManager.java
│       │   └── client/NettySyncClient.java
│       └── util/
│           ├── DownloadFileInfo.java        # 下行同步 DTO
│           └── SyncStyle.java
│
├── sync-app/                        # Electron + Vue 3 桌面端
│   └── src/renderer/src/
│       ├── views/
│       │   ├── DashBoard.vue        # 同步任务管理 + 群组共享文件入口
│       │   ├── FileExplorer.vue     # 本地文件树浏览
│       │   ├── GroupPage.vue        # 群组管理（创建/删除群组、管理成员和共享文件夹）
│       │   ├── GroupExplorer.vue    # 群组共享文件浏览（路径导航、下行同步）
│       │   ├── LogPage.vue          # 操作日志
│       │   ├── Login.vue
│       │   └── Register.vue
│       ├── components/
│       │   └── Profilemodal.vue
│       └── utils/request.js         # Axios 封装
│
├── Database Tables.md               # 数据库表结构文档
├── API.md                           # 接口文档
└── README.md
```

---

## 快速开始

### 前置要求

- Java 21+
- Maven 3.8+
- Node.js 18+ / npm
- MySQL 8.0+
- Redis 6+

### 1. 配置服务端

编辑 `server/src/main/resources/application-dev.yml`，填写：

```yaml
application:
  datasource:
    mysql:
      url: jdbc:mysql://localhost:3306/datasync
      username: root
      password: your_password
  redis:
    master:
      host: localhost
      port: 6379
      password: ""
  netty:
    server:
      port: 8888
      basePath: /data/datasync/storage
  jwt:
    secretkey: your-256-bit-secret
    accessTokenExpiration: 3600000
    freshTokenExpiration: 86400000
    token-name: Authorization
    uid: uid
    role: role
```

生成 RSA 密钥对并放置于 `server/conf/`：

```bash
openssl genrsa -out conf/rsa-private.pem 2048
openssl rsa -in conf/rsa-private.pem -pubout -out conf/rsa-public.pem
```

启动：

```bash
cd server
mvn spring-boot:run
```

### 2. 配置客户端代理

编辑 `client-app/src/main/resources/application-dev.yml`：

```yaml
application:
  datasource:
    sqlite:
      url: jdbc:sqlite:./db.sqlite
  netty:
    client:
      host: localhost      # Server Netty 地址
      port: 8888
    server:
      port: 9999           # 预留（未来双向 Netty 使用）
      basePath: ""
  jwt:
    # 与 server 保持一致
    secretkey: your-256-bit-secret
    ...
```

将 `server/conf/rsa-public.pem` 复制到 `client-app/conf/rsa-public.pem`。

启动：

```bash
cd client-app
mvn spring-boot:run
```

### 3. 启动桌面客户端

```bash
cd sync-app
npm install
npm run dev        # 开发模式（带热重载）
npm run build      # 生产打包
```

---

## 功能说明

### 同步任务管理（Dashboard）

- **新建任务**：选择本地路径、CDC 算法、远端主机、定时策略
- **修改任务**：更新任何配置项；路径变化时自动重新扫描文件树
- **删除任务**：同时清除本地 SQLite 中的 File + SubFile 记录；若该任务文件夹已被共享至有成员的群组，则拒绝删除并在弹窗中提示用户先删除群组
- **上行同步**：触发 CDC 分块 → 差量比对 → Netty 增量传输
- **下行同步**：拉取服务端全量文件 → 本地覆盖

### 文件浏览器（FileExplorer）

- 分级浏览文件夹，支持面包屑导航
- 网格 / 列表双视图切换
- 右键上下文菜单：打开、上行同步、下行同步、删除
- 实时展示各文件同步状态（已同步 / 待同步）

### 群组管理（GroupPage）

- 创建 / 删除群组
- 通过邮箱添加 / 移除成员
- 为群组添加 / 移除共享文件夹（scope 键 = `ownerEmail/folderName`，UI 仅显示 `folderName` 部分）
- 成员标签区分群组创建者与普通成员

### 群组文件浏览器（GroupExplorer）

- 从 Dashboard 群组区域点击任意 scope 卡片进入
- 路径导航：双击文件夹进入子目录，面包屑一键跳转
- 网格 / 列表双视图切换，蓝色状态点标识服务端文件
- 工具栏「下行同步」按钮：异步将当前 scope 下载到指定本地路径
- 右键菜单：进入文件夹 / 下行同步到本地

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

> **注意**：请勿将 `application-dev.yml` 或 `application-prod.yml` 提交到版本控制，这些文件包含敏感凭据。请使用仓库提供的 `.example` 文件作为配置模板。

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
