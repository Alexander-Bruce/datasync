# DataSync 数据库与存储文档

**[English](Database%20Tables.en.md) | 中文**

DataSync 使用三类存储：

1. 中心 `server` 使用 MySQL/MariaDB。
2. 本地 `client-app` 使用 SQLite。
3. 服务端 `basePath` 下的 JSON 与文件系统存储。

---

## 1. 服务端 MySQL / MariaDB

初始化脚本：

```text
server/src/main/resources/db/mysql-init.sql
```

### 1.1 `user`

```sql
CREATE TABLE IF NOT EXISTS user (
  id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(191) NOT NULL,
  email VARCHAR(191) NOT NULL,
  password VARCHAR(255) NOT NULL,
  avatar TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  KEY idx_user_username (username)
);
```

| 字段         | 类型                 | 可空 | 说明                                                     |
| ------------ | -------------------- | ---- | -------------------------------------------------------- |
| `id`         | `INT AUTO_INCREMENT` | 否   | 服务端用户 ID。该值必须同步保存到本地 SQLite `User.id`。 |
| `username`   | `VARCHAR(191)`       | 否   | 用户显示名。                                             |
| `email`      | `VARCHAR(191)`       | 否   | 唯一邮箱。同步和群组接口都把邮箱作为主要身份 key。       |
| `password`   | `VARCHAR(255)`       | 否   | BCrypt hash 后的密码。                                   |
| `avatar`     | `TEXT`               | 是   | 头像 URL 或空值。不应存储大体积图片原始字节。            |
| `created_at` | `TIMESTAMP`          | 否   | 创建时间。                                               |
| `updated_at` | `TIMESTAMP`          | 否   | 更新时间。                                               |

索引：

| 索引                | 字段       | 用途                                      |
| ------------------- | ---------- | ----------------------------------------- |
| `PRIMARY KEY`       | `id`       | 按数字 ID 查询用户，也是 JWT subject。    |
| `uk_user_email`     | `email`    | 防止重复账户；群组成员使用 email 字符串。 |
| `idx_user_username` | `username` | 支持用户名搜索。                          |

映射文件：

```text
server/src/main/java/backend/model/entity/User.java
server/src/main/java/backend/mapper/mysql/UserMapper.java
server/src/main/resources/backend/mapper/mysql/UserMapper.xml
```

说明：

- 密码使用 BCrypt strength 12。
- `searchByQuery` 用于群组成员搜索/自动补全。
- `avatar` 应是远端可访问 URL。用户上传 base64 头像时，服务端会把文件写入 `basePath/avatars`，MySQL 中保存 public URL 字符串。

---

## 2. 客户端 SQLite

建表脚本：

```text
client-app/src/main/resources/backend/mapper/sqlite/scheme.sql
```

默认本地数据库：

```text
datasync-user.db
```

配置：

```yaml
application:
  datasource:
    sqlite:
      url: jdbc:sqlite:datasync-user.db?journal_mode=WAL&busy_timeout=5000
```

### 2.1 `User`

```sql
CREATE TABLE IF NOT EXISTS User (
    id INTEGER,
    username TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    avatar TEXT,
    ip TEXT,
    user_agent TEXT,
    refresh_token TEXT,
    access_token TEXT,
    PRIMARY KEY(username)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique ON User(email);
```

| 字段            | 类型      | 可空                        | 说明                                   |
| --------------- | --------- | --------------------------- | -------------------------------------- |
| `id`            | `INTEGER` | schema 允许空，实际必须有值 | 服务端用户 ID。任务操作依赖该字段。    |
| `username`      | `TEXT`    | 否                          | 用户显示名。当前 schema 主键。         |
| `email`         | `TEXT`    | 否                          | 唯一邮箱。本地查找/upsert 使用该字段。 |
| `avatar`        | `TEXT`    | 是                          | 头像 URL 或空字符串。                  |
| `ip`            | `TEXT`    | 是                          | 预留/本地元数据字段。                  |
| `user_agent`    | `TEXT`    | 是                          | 预留/本地元数据字段。                  |
| `refresh_token` | `TEXT`    | 是                          | 服务端登录/注册返回的 JWT token 缓存。 |
| `access_token`  | `TEXT`    | 是                          | 预留 token 字段。                      |

重要不变量：

- `User.email` 是本地唯一身份 key。
- `User.id` 必须保存中心服务端 ID。如果为空，`File.user_id` 相关任务查询会异常。

映射文件：

```text
client-app/src/main/java/backend/model/entity/User.java
client-app/src/main/java/backend/mapper/sqlite/UserMapper.java
client-app/src/main/resources/backend/mapper/sqlite/UserMapper.xml
```

### 2.2 `File`

```sql
CREATE TABLE IF NOT EXISTS File (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    alias TEXT,
    path TEXT,
    remote_host TEXT,
    scheduled TEXT,
    cdc_alg TEXT CHECK(cdc_alg IN ('FastCDC','FlipCDC','QuickCDC','RabinCDC')),
    is_dir BOOLEAN DEFAULT 0,
    is_sync BOOLEAN DEFAULT 0,
    description TEXT,
    update_time TEXT,
    user_id INTEGER
);

CREATE INDEX IF NOT EXISTS idx_file_user_id ON File(user_id);
```

| 字段          | 类型                                | 可空 | 说明                                                                |
| ------------- | ----------------------------------- | ---- | ------------------------------------------------------------------- |
| `id`          | `INTEGER PRIMARY KEY AUTOINCREMENT` | 否   | 本地同步任务 ID。                                                   |
| `alias`       | `TEXT`                              | 是   | 用户可见任务名。业务逻辑要求同一用户内唯一。                        |
| `path`        | `TEXT`                              | 是   | 本地任务根文件/文件夹的绝对路径。                                   |
| `remote_host` | `TEXT`                              | 是   | 元数据/旧版 host 字段。运行时 HTTP URL 来自客户端配置。             |
| `scheduled`   | `TEXT`                              | 是   | 上传间隔，例如 `5m`、`1h`、`1d`、纯数字分钟、空或 `never`。         |
| `cdc_alg`     | `TEXT`                              | 是   | `FastCDC`、`FlipCDC`、`QuickCDC`、`RabinCDC`。                      |
| `is_dir`      | `BOOLEAN`                           | 否   | `path` 是否为目录任务。                                             |
| `is_sync`     | `BOOLEAN`                           | 否   | 本地状态是否被认为已同步。                                          |
| `description` | `TEXT`                              | 是   | 任务描述。                                                          |
| `update_time` | `TEXT`                              | 是   | 任务更新时间。文件监听代码兼容 Java ISO 和 SQLite datetime 字符串。 |
| `user_id`     | `INTEGER`                           | 是   | 来自本地 `User.id` 的服务端用户 ID。                                |

业务行为：

- `updateFileTask()` 校验 alias/path 非空。
- 同一用户内 alias 重复会返回冲突。
- path 改变会在事务提交后重新扫描 `SubFile`。
- 上传/下载优先按 `fileId` 定位任务，仍保留按 path 回退的兼容逻辑。

映射文件：

```text
client-app/src/main/java/backend/model/entity/File.java
client-app/src/main/java/backend/mapper/sqlite/FileMapper.java
client-app/src/main/resources/backend/mapper/sqlite/FileMapper.xml
```

### 2.3 `SubFile`

```sql
CREATE TABLE IF NOT EXISTS SubFile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    parent INTEGER,
    name TEXT,
    relative_path TEXT,
    depth INTEGER,
    is_dir BOOLEAN DEFAULT 0,
    is_sync BOOLEAN DEFAULT 0,
    FOREIGN KEY(file_id) REFERENCES File(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_subfile_file_id ON SubFile(file_id);
```

| 字段            | 类型                                | 可空 | 说明                                                 |
| --------------- | ----------------------------------- | ---- | ---------------------------------------------------- |
| `id`            | `INTEGER PRIMARY KEY AUTOINCREMENT` | 否   | 本地文件树行 ID。                                    |
| `file_id`       | `INTEGER`                           | 否   | 所属同步任务 ID。                                    |
| `parent`        | `INTEGER`                           | 是   | 父级 `SubFile.id`；根级子项为 null。                 |
| `name`          | `TEXT`                              | 是   | 文件或文件夹名称。                                   |
| `relative_path` | `TEXT`                              | 是   | 相对于 `File.path` 的路径，服务代码中使用 `/` 分隔。 |
| `depth`         | `INTEGER`                           | 是   | 相对于任务根的树深度。根级子项 depth 为 0。          |
| `is_dir`        | `BOOLEAN`                           | 否   | 是否为目录。                                         |
| `is_sync`       | `BOOLEAN`                           | 否   | 该条目是否被认为已同步。                             |

业务行为：

- `addFiles()` 使用 BFS 扫描，保证父目录先于子项插入。
- 目录任务会把根目录的直接子项记录为 `SubFile`。
- 单文件任务也会为文件本身创建一条 `SubFile`。
- `saveSubFileListInTransaction()` 会重建某个任务的 `SubFile`。
- `refreshSubFiles()` 只插入磁盘上新增但 DB 中缺失的行，不删除已有行。

映射文件：

```text
client-app/src/main/java/backend/model/entity/SubFile.java
client-app/src/main/java/backend/mapper/sqlite/SubFileMapper.java
client-app/src/main/resources/backend/mapper/sqlite/SubFileMapper.xml
```

---

## 3. 服务端文件系统存储

配置：

```yaml
application:
  netty:
    server:
      basePath: /sync
```

布局：

```text
basePath/
|-- <email>/
|   `-- <alias>/
|       `-- <rootName>/
|           `-- <relative files>
|-- avatars/
|   `-- <userId>.<png|jpg|webp|gif>
`-- groups.json
```

### 3.1 Scope Key

标准 scope key：

```text
<email>/<alias>/<rootName>
```

示例：

```text
alice@example.com/Work/Documents
alice@example.com/SingleFile/budget.xlsx
```

该 scope key 被以下功能共同使用：

- 上传 compare。
- 下载列表/内容。
- 群组共享 scope。
- 任务删除保护。
- 远端任务恢复。

不要用 `Documents` 这类展示名替代，也不要回退到旧的 `email/rootName` 布局。

### 3.2 上传 Storage Path

上传时客户端发送：

```text
storagePath = <email>/<alias>/<relative directory>
fileName = <file name only>
```

服务端写入：

```text
basePath/storagePath/fileName
```

服务端会拒绝：

- 绝对路径。
- 空 path segment。
- `.` 或 `..`。
- Null byte。
- 包含 `/` 或 `\` 的文件名。

### 3.3 上传临时文件

上传先写：

```text
<fileName>.part
```

再替换：

```text
<fileName>
```

下载/列表接口会跳过 `.part` 文件。

---

## 4. `groups.json`

存储位置：

```text
<basePath>/groups.json
```

结构：

```json
[
  {
    "id": "group-uuid",
    "name": "Team Docs",
    "ownerEmail": "alice@example.com",
    "admins": ["bob@example.com"],
    "members": ["carol@example.com"],
    "scopes": ["alice@example.com/Work/Documents"]
  }
]
```

| 字段         | 类型     | 说明                                                |
| ------------ | -------- | --------------------------------------------------- |
| `id`         | string   | 创建群组时生成的 UUID。                             |
| `name`       | string   | 群组名。                                            |
| `ownerEmail` | string   | 唯一 owner。Owner 不重复放入 `admins`。             |
| `admins`     | string[] | 管理员邮箱。管理员可管理普通成员和 scope。          |
| `members`    | string[] | 普通成员邮箱。                                      |
| `scopes`     | string[] | 共享远端 scope key，格式为 `email/alias/rootName`。 |

并发：

- `GroupServiceImpl` 使用 `ReentrantLock` 保护读写。
- 新增 mutation 必须让 read-modify-write 处在同一把锁内。

完整性：

- 添加 member/admin 时会校验邮箱是否存在于 MySQL。
- Owner 不能通过 admin 操作被移除。
- 移除 admin 时会在适当情况下降级为 member。
- 任务删除前会检查该文件，避免删除仍被群组引用的远端 scope。

---

## 5. 派生响应模型

### 5.1 `GroupInfo`

由 `/client/group/files` 和 `/server/group/files` 返回。

```json
{
  "id": "group-uuid",
  "name": "Team Docs",
  "ownerEmail": "alice@example.com",
  "owner": true,
  "admins": ["bob@example.com"],
  "members": ["carol@example.com"],
  "scopes": []
}
```

### 5.2 `GroupScopeInfo`

```json
{
  "scopeName": "alice@example.com/Work/Documents",
  "alias": "Work",
  "rootName": "Documents",
  "displayName": "Documents",
  "files": []
}
```

### 5.3 `GroupFileNode`

```json
{
  "name": "report.docx",
  "relativePath": "subdir/report.docx",
  "dir": false
}
```

### 5.4 `RemoteScope`

由 `/server/file/list-scopes` 返回。

```json
{
  "alias": "Work",
  "rootName": "Documents",
  "isDir": true,
  "scopeName": "alice@example.com/Work/Documents"
}
```

---

## 6. 迁移说明

服务端目录下仍有一个旧的 SQLite schema：

```text
server/src/main/resources/backend/mapper/sqlite/scheme.sql
```

该文件不是当前中心服务端运行时的权威 schema。当前服务端身份数据使用 MySQL/MariaDB，初始化脚本是 `server/src/main/resources/db/mysql-init.sql`。

Docker profile 可通过 `ScopeStorageMigrationRunner` 将旧远端存储布局迁移到 `email/alias/rootName` 布局。手动触发接口：

```http
POST /server/file/migrate-legacy-storage
```

---

## 7. Git 忽略的运行时文件

以下运行/生成文件有意忽略：

- `groups.json`
- `*.db`、`*.sqlite`、`*.sqlite-wal`、`*.sqlite-shm`
- `log/`、`logs/`、`*.log`
- `server/target/`、`client-app/target/`
- `sync-app/out/`、`sync-app/dist/`、桌面安装包
- `*.pem`、`*.key`、`*.p12` 等私钥和证书

不要提交真实凭据或本地运行时数据库。
