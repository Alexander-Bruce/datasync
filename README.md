# DataSync

**[English](README.md) | [中文](README.zh.md)**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-39-47848f.svg)](https://www.electronjs.org/)

A distributed file synchronization and backup system built on Content-Defined Chunking (CDC) algorithms. DataSync supports incremental transfer, end-to-end encryption, group-based file sharing, and ships with a cross-platform Electron desktop client.

---

## Screenshots

| Dashboard | File Explorer |
|-----------|--------------|
| ![Dashboard](docs/screenshots/dashboard.png) | ![File Explorer](docs/screenshots/file-explorer-grid.png) |

| Group Management | Group File Browser |
|-----------------|-------------------|
| ![Group Management](docs/screenshots/group-management.png) | ![Group Explorer](docs/screenshots/group-explorer.png) |

| Log Page | Sync Algorithm Selection |
|----------|--------------------------|
| ![Log Page](docs/screenshots/log-page.png) | ![Sync Algorithm](docs/screenshots/sync-algorithm.png) |

<details>
<summary>More screenshots</summary>

| Login | Register |
|-------|----------|
| ![Login](docs/screenshots/login.png) | ![Register](docs/screenshots/register.png) |

| File List View | Account Settings |
|---------------|-----------------|
| ![File List](docs/screenshots/file-explorer-list.png) | ![Account Settings](docs/screenshots/account-settings.png) |

</details>

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Directory Structure](#directory-structure)
- [Quick Start](#quick-start)
- [Configuration Reference](#configuration-reference)
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
- **HTTPS File Sync**: Uploads and downloads file content through the server HTTP API, so it works behind Hugging Face Spaces' HTTPS proxy
- **Legacy Netty Transport**: Netty sync classes remain available for self-hosted deployments that expose raw TCP ports
- **Local Metadata**: SQLite on the client side for managing file trees and sync state
- **Scheduled Sync**: Cron expression support for automated sync policies
- **Group Sharing**: Add members by email, share sync task files or folders to groups; members can browse and pull down
- **Multi-Task Isolated Storage**: Server stores files under `email/alias/rootName` namespaces, preventing cross-task and cross-user collisions
- **Shared Deletion Guard**: Deleting a sync task is blocked while its folder is shared to a group with active members

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
│  HTTP file upload/download │ legacy Netty client              │
└──────┬──────────────────────────────────────┬────────────────┘
       │ REST + file content (HTTP, port 8090)
       ▼                                      ▼
┌──────────────────────────────────────────────────────────────┐
│                   server (Spring Boot)                        │
│  MySQL │ FileService │ ServerSyncController │ GroupController │
│  HTTP file storage │ legacy Netty server                      │
└──────────────────────────────────────────────────────────────┘
```

For full details see [ARCHITECTURE.md](./ARCHITECTURE.md).

**Upload Sync Flow**

1. Frontend triggers `POST /client/sync/upload`
2. Client chunks local files using the selected CDC algorithm and computes SHA-256 hashes
3. Client calls `POST /server/file/compare`; server returns the list of delta chunks
4. Client uploads changed files to `POST /server/file/upload` over the configured server HTTP URL
5. Server writes each file through a `.part` temp file and atomic rename, then removes deleted files during compare
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
| Netty       | —        | Legacy raw TCP sync transport for self-hosted deployments |
| JJWT        | 0.12.5   | JWT authentication          |
| MySQL       | 8.0+     | Server persistent storage   |
| SQLite      | —        | Client local metadata       |
| Redis       | 6+       | Server-side caching         |
| Lombok      | —        | Boilerplate code generation |

### Frontend (sync-app)

| Technology   | Version | Purpose                              |
| ------------ | ------- | ------------------------------------ |
| Vue 3        | 3.5     | Frontend framework (Composition API) |
| Vue Router   | 4       | Client-side routing                  |
| Electron     | 39      | Desktop application container        |
| Tailwind CSS | 3       | Utility-first CSS framework          |
| Axios        | —       | HTTP client                          |

---

## Directory Structure

```
datasync/
├── server/                          # Spring Boot server
│   └── src/main/java/
│       ├── backend/
│       │   ├── controller/          # REST endpoints (auth, sync, group, user)
│       │   ├── service/             # Business logic
│       │   ├── model/               # Entities and DTOs
│       │   ├── sync/                # Netty server (NettySyncServer, SyncServerHandler)
│       │   └── util/
│       └── dataSync/                # CDC algorithm implementations
│           ├── FastCDC/
│           ├── FlipCDC/
│           ├── QuickCDC/
│           └── RabinCDC/
│
├── client-app/                      # Spring Boot client agent
│   └── src/main/java/
│       ├── backend/
│       │   ├── controller/          # REST endpoints (file, sync, group, user, log)
│       │   ├── service/             # Business logic + file tree management
│       │   ├── mapper/sqlite/       # MyBatis SQLite mappers
│       │   ├── sync/                # Netty client (NettySyncClient, NettyClientManager)
│       │   └── task/                # Scheduled sync + file watcher
│       └── dataSync/                # CDC algorithm implementations (same as server)
│
├── sync-app/                        # Electron + Vue 3 desktop
│   └── src/renderer/src/
│       ├── views/                   # Dashboard, FileExplorer, GroupPage, GroupExplorer, LogPage, Login, Register
│       ├── components/
│       └── utils/request.js         # Axios wrapper
│
├── docs/screenshots/                # UI screenshots
├── API.en.md                        # REST API documentation
├── Database Tables.en.md            # Database schema documentation
└── ARCHITECTURE.md                  # System architecture details
```

---

## Quick Start

### Prerequisites

| Requirement | Version  | Notes |
|-------------|----------|-------|
| Java JDK    | 21+      | Required for server and client-app |
| Maven       | 3.8+     | Or use the included `mvnw` wrapper |
| Node.js     | 18+      | Required for sync-app |
| MySQL       | 8.0+     | Server-side persistent storage |
| Redis       | 6+       | Server-side caching |
| OpenSSL     | any      | For RSA key pair generation |

---

### 1. Clone the repository

```bash
git clone https://github.com/Alexander-Bruce/datasync.git
cd datasync
git config core.hooksPath .githooks   # enable pre-commit format check
```

---

### 2. Set up the Server

**Create the database schema** (MySQL):

```sql
CREATE DATABASE datasync CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Copy the example config and fill in your values:**

```bash
cp server/src/main/resources/application-dev.yml.example \
   server/src/main/resources/application-dev.yml
```

Edit `server/src/main/resources/application-dev.yml` — see [Configuration Reference](#configuration-reference) below.

**Optional: generate RSA keys for legacy Netty transport:**

```bash
mkdir -p server/conf
openssl genrsa -out server/conf/rsa-private.pem 2048
openssl rsa -in server/conf/rsa-private.pem -pubout -out server/conf/rsa-public.pem
```

**Start the server:**

```bash
cd server
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

The server starts on **port 8090** for HTTP APIs. Legacy Netty defaults to **port 8080** when used in a self-hosted deployment.

---

### 3. Set up the Client Agent

**Copy the example config:**

```bash
cp client-app/src/main/resources/application-dev.yml.example \
   client-app/src/main/resources/application-dev.yml
```

Edit `client-app/src/main/resources/application-dev.yml` — see [Configuration Reference](#configuration-reference).

**Copy the server's public key:**

```bash
mkdir -p client-app/conf
cp server/conf/rsa-public.pem client-app/conf/rsa-public.pem
```

**Start the client agent:**

```bash
cd client-app
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

The client agent starts on **port 8092**.

---

### 4. Start the Desktop UI

```bash
cd sync-app
npm install
npm run dev        # Development mode with hot reload
npm run build      # Package for distribution
```

Open the Electron window and log in with your registered account.

---

## Configuration Reference

### Server (`server/src/main/resources/application-dev.yml`)

```yaml
application:
  datasource:
    mysql:
      url: jdbc:mysql://<host>:3306/datasync   # MySQL JDBC URL
      username: <your_mysql_username>
      password: <your_mysql_password>

  redis:
    master:
      host: <redis_host>
      port: 6379
      password: <your_redis_password>
    slave:
      host: <redis_slave_host>      # Can be same as master for single-node
      port: 6380
      password: <your_redis_password>

  aws:
    s3:
      accesskey: <your_s3_access_key>   # Optional: only needed for S3 storage backend
      secretkey: <your_s3_secret_key>
      region: auto
      endpoint: <your_s3_compatible_endpoint>
      bucket: <your_bucket_name>
      path-style-access: true

  netty:
    server:
      port: 8080                         # Legacy Netty port for self-hosted TCP sync
      basePath: /path/to/server/storage  # Where synced files are stored on server

  jwt:
    secretkey: <base64_encoded_256bit_secret>  # Generate: openssl rand -base64 32
    freshTokenExpiration: 2592000000            # Refresh token TTL (ms), default 30 days
    accessTokenExpiration: 25920000000          # Access token TTL (ms), default 300 days
    token-name: Authorization
    uid: uid
    role: role

  mybatis:
    type-aliases-package: backend.model.entity
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### Client Agent (`client-app/src/main/resources/application-dev.yml`)

```yaml
application:
  datasource:
    sqlite:
      url: jdbc:sqlite:datasync-user.db?journal_mode=WAL&busy_timeout=5000

  netty:
    server:
      port: 8080                           # Legacy Netty port for self-hosted TCP sync
      basePath: /path/to/local/sync/root   # Local root for synced files
    client:
      port: 8080                           # Legacy Netty port for self-hosted TCP sync
      host: <server_ip_or_hostname>        # Address of the server

  jwt:
    secretkey: <same_secret_as_server>     # Must be identical to server's JWT secret
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

> **Tip:** Generate a JWT secret key with: `openssl rand -base64 32`

---

## Features

### Sync Task Management (Dashboard)

- **Create task**: Choose local path, CDC algorithm, remote host, and schedule
- **Edit task**: Update any config; path changes trigger automatic file tree rescan
- **Delete task**: Clears File + SubFile records in SQLite; blocked if the folder is shared to a group with active members
- **Upload sync**: CDC chunking → delta comparison → HTTPS file upload
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

- Path navigation: double-click folders to descend, breadcrumb to jump back
- Grid / list view toggle
- Download button: asynchronously downloads the current scope to a local path

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

> **Important:** Never commit `application-dev.yml` or `application-prod.yml` to version control — they contain secrets. Both files are listed in `.gitignore`. Use the provided `.example` files as templates.

---

## Contributing

1. Fork the repository
2. Clone and enable the pre-commit hook:
   ```bash
   git clone https://github.com/<you>/datasync.git
   cd datasync
   git config core.hooksPath .githooks
   ```
3. Create a feature branch: `git checkout -b feature/my-feature`
4. Commit your changes: `git commit -m 'feat: add my feature'`
5. Push to the branch: `git push origin feature/my-feature`
6. Open a pull request

The pre-commit hook automatically checks code format before each commit:
- **Java**: [Google Java Format](https://google.github.io/styleguide/javaguide.html) via Spotless — fix with `cd server && ./mvnw spotless:apply`
- **Frontend**: Prettier + ESLint — fix with `cd sync-app && npm run format`

---

## License

This project is licensed under the [MIT License](LICENSE).
