# DataSync

**[English](README.md) | [中文](README.zh.md)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-39-47848f.svg)](https://www.electronjs.org/)

A distributed file synchronization and backup system built on Content-Defined Chunking (CDC) algorithms. DataSync supports incremental transfer, end-to-end encryption, group-based file sharing, and ships with a cross-platform Electron desktop client.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Directory Structure](#directory-structure)
- [Quick Start](#quick-start)
- [Features](#features)
- [API Documentation](#api-documentation)
- [Database Design](#database-design)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

DataSync consists of three sub-modules:

| Module       | Description                                              | Default Port |
| ------------ | -------------------------------------------------------- | ------------ |
| `server`     | Spring Boot backend — stores synced files, exposes REST  | 8090         |
| `client-app` | Spring Boot client agent — manages local file metadata   | 8092         |
| `sync-app`   | Vue 3 + Electron desktop UI                              | —            |

**Core Features**

- **CDC Incremental Sync**: Supports FastCDC, FlipCDC, QuickCDC, and RabinCDC — only changed chunks are transferred
- **Bidirectional Sync**: Upload (local → server) and download (server → local, direct overwrite)
- **Encrypted Transfer**: AES-256-GCM data encryption + RSA-2048-OAEP key exchange
- **High-Performance Netty Transport**: Asynchronous socket communication via Netty
- **Local Metadata**: SQLite on the client side for managing file trees and sync state
- **Scheduled Sync**: Cron expression support for automated sync policies
- **Group Sharing**: Add members by email, share sync task folders to groups; members can browse and pull down
- **Multi-User Isolated Storage**: Server stores files under `email/folderName` namespaces, preventing cross-user collisions for identically named folders
- **Shared Deletion Guard**: Deleting a sync task is blocked while its folder is shared to a group with active members; the group must be deleted or all members removed first

---

## Architecture

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
│  GroupService → groups.json                                   │
│  NettySyncServer ← SyncServerHandler                         │
└──────────────────────────────────────────────────────────────┘
```

For a detailed explanation of system boundaries, data flows, and invariants, see [ARCHITECTURE.md](./ARCHITECTURE.md).

**Upload Sync Flow**

1. Frontend triggers `POST /client/sync/upload`
2. Client chunks local files using the selected CDC algorithm and computes SHA-256 hashes
3. Client calls `POST /server/file/compare`; server returns the list of delta chunks
4. Client sends delta chunks (AES-GCM encrypted) to the server via Netty
5. Server reconstructs each file (temp file → atomic rename) and removes deleted files
6. Client updates local SQLite `is_sync = true`

**Download Sync Flow**

1. Frontend triggers `POST /client/sync/download`
2. Client calls `POST /server/file/download`; server scans the corresponding scope directory
3. Server returns all files Base64-encoded
4. Client writes each file locally (direct overwrite)
5. Client updates local SQLite `is_sync = true`

---

## Tech Stack

### Backend (server & client-app)

| Technology  | Version  | Purpose                     |
| ----------- | -------- | --------------------------- |
| Java        | 21       | Programming language        |
| Spring Boot | 3.4.2    | Web framework               |
| MyBatis     | 3.0.4    | ORM / SQL mapping           |
| Netty       | —        | High-performance async I/O  |
| JJWT        | 0.12.5   | JWT authentication          |
| MySQL       | 8.0+     | Server persistent storage   |
| SQLite      | —        | Client local metadata       |
| Redis       | 6+       | Server-side caching         |
| Lombok      | —        | Boilerplate code generation |

### Frontend (sync-app)

| Technology  | Version | Purpose                              |
| ----------- | ------- | ------------------------------------ |
| Vue 3       | 3.5     | Frontend framework (Composition API) |
| Vue Router  | 4       | Client-side routing                  |
| Electron    | 39      | Desktop application container        |
| Tailwind CSS| 3       | Utility-first CSS framework          |
| Axios       | —       | HTTP client                          |
| Prettier    | 3       | Code formatter                       |

---

## Directory Structure

```
datasync/
├── server/                          # Spring Boot server
│   └── src/main/java/backend/
│       ├── controller/
│       │   ├── UnAuthController.java        # Login / Register (no auth)
│       │   ├── GroupController.java         # Group management endpoints
│       │   ├── client/SyncController.java   # Upload sync trigger
│       │   └── server/ServerSyncController.java
│       ├── service/
│       │   ├── FileService.java
│       │   └── GroupService.java
│       ├── model/
│       │   ├── entity/User.java
│       │   ├── Group.java
│       │   └── GroupFileNode.java
│       ├── sync/                    # Netty server
│       │   ├── server/NettySyncServer.java
│       │   └── server/SyncServerHandler.java
│       └── src/main/java/dataSync/ # CDC algorithm implementations
│           ├── CDCManager.java
│           ├── FastCDC/
│           ├── FlipCDC/
│           ├── QuickCDC/
│           └── RabinCDC/
│
├── client-app/                      # Spring Boot client agent
│   └── src/main/java/backend/
│       ├── controller/
│       │   ├── FileController.java
│       │   ├── GroupController.java
│       │   ├── SyncController.java
│       │   └── UserController.java
│       ├── service/FileService.java
│       ├── mapper/sqlite/
│       │   ├── FileMapper.java
│       │   └── SubFileMapper.java
│       ├── sync/                    # Netty client
│       │   ├── NettyClientManager.java
│       │   └── client/NettySyncClient.java
│       └── task/                   # Scheduled sync & file watcher
│
├── sync-app/                        # Electron + Vue 3 desktop
│   └── src/renderer/src/
│       ├── views/
│       │   ├── DashBoard.vue        # Sync task management
│       │   ├── FileExplorer.vue     # Local file tree browser
│       │   ├── GroupPage.vue        # Group management
│       │   ├── GroupExplorer.vue    # Group shared file browser
│       │   ├── LogPage.vue          # Operation logs
│       │   ├── Login.vue
│       │   └── Register.vue
│       └── utils/request.js         # Axios wrapper
│
├── API.en.md                        # REST API documentation
├── Database Tables.en.md            # Database schema documentation
├── ARCHITECTURE.md                  # System architecture details
└── AGENTS.md                        # AI agent operation notes
```

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 18+ / npm
- MySQL 8.0+
- Redis 6+

### 1. Configure the Server

Copy the example config and fill in your values:

```bash
cp server/src/main/resources/application-dev.yml.example \
   server/src/main/resources/application-dev.yml
```

Edit `application-dev.yml` with your MySQL, Redis, Netty, and JWT settings. Key fields:

```yaml
application:
  datasource:
    mysql:
      url: jdbc:mysql://localhost:3306/datasync
      username: your_mysql_user
      password: your_mysql_password
  redis:
    master:
      host: localhost
      port: 6379
      password: your_redis_password
  aws:
    s3:
      accesskey: your_s3_access_key   # optional, for S3 storage backend
      secretkey: your_s3_secret_key
  netty:
    server:
      port: 8443
      basePath: /path/to/server/storage
  jwt:
    secretkey: your_base64_encoded_256bit_secret
```

Generate an RSA key pair for encrypted Netty transport:

```bash
mkdir -p server/conf
openssl genrsa -out server/conf/rsa-private.pem 2048
openssl rsa -in server/conf/rsa-private.pem -pubout -out server/conf/rsa-public.pem
```

Start the server:

```bash
cd server
./mvnw spring-boot:run
```

### 2. Configure the Client Agent

```bash
cp client-app/src/main/resources/application-dev.yml.example \
   client-app/src/main/resources/application-dev.yml
```

Edit `application-dev.yml` and copy the server's public key:

```bash
cp server/conf/rsa-public.pem client-app/conf/rsa-public.pem
```

Start the client agent:

```bash
cd client-app
./mvnw spring-boot:run
```

### 3. Start the Desktop Client

```bash
cd sync-app
npm install
npm run dev        # Development mode (with hot reload)
npm run build      # Production build
```

---

## Features

### Sync Task Management (Dashboard)

- **Create task**: Choose local path, CDC algorithm, remote host, and schedule
- **Edit task**: Update any config; path changes trigger automatic file tree rescan
- **Delete task**: Clears File + SubFile records in SQLite; blocked if the folder is shared to a group with active members
- **Upload sync**: CDC chunking → delta comparison → Netty incremental transfer
- **Download sync**: Pulls all files from the server → overwrites local copies

### File Browser (FileExplorer)

- Hierarchical folder browsing with breadcrumb navigation
- Grid / list view toggle
- Right-click context menu: open, upload sync, download sync, delete
- Real-time sync status per file (synced / pending)

### Group Management (GroupPage)

- Create / delete groups
- Add / remove members by email
- Add / remove shared folders for a group
- Member tags distinguish group owner from regular members

### Group File Browser (GroupExplorer)

- Enter by clicking any scope card in the Dashboard group area
- Path navigation: double-click folders to descend, breadcrumb to jump back
- Grid / list view toggle
- Download button: asynchronously downloads the current scope to a local path
- Right-click menu: enter folder / download to local

---

## API Documentation

See [API.en.md](./API.en.md)

---

## Database Design

See [Database Tables.en.md](./Database%20Tables.en.md)

---

## Security

| Mechanism           | Implementation                                     |
| ------------------- | -------------------------------------------------- |
| Authentication      | JWT (Access Token + Refresh Token)                 |
| Password storage    | BCrypt (strength 12)                               |
| Transfer encryption | AES-256-GCM (independent IV per packet)            |
| Key exchange        | RSA-2048 OAEP-SHA256                               |
| CORS control        | Spring Security CORS configuration                 |
| Session policy      | Stateless (no server-side session)                 |

> **Note**: Never commit `application-dev.yml` or `application-prod.yml` to version control — they contain secrets. Use the provided `.example` files as templates.

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'feat: add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a pull request

Please follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for Java code and run `prettier` on frontend changes before submitting.

---

## License

This project is licensed under the [MIT License](LICENSE).
