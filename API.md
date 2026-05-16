# DataSync API 文档

**[English](API.en.md) | 中文**

本文档描述当前 `server` 与 `client-app` 暴露的 REST API。

---

## 基础约定

| 服务         | Base URL                                                                          | 说明                                                 |
| ------------ | --------------------------------------------------------------------------------- | ---------------------------------------------------- |
| `server`     | 本地 `http://localhost:8090`，Hugging Face Spaces 上为 `https://<space>.hf.space` | 中心服务，负责用户、远端文件、群组、头像和健康检查。 |
| `client-app` | `http://127.0.0.1:8092`                                                           | Electron 渲染进程调用的本地桌面代理。                |

除特别说明外：

- 请求体均为 JSON。
- `client-app` 返回统一 `ResultEntity` 响应包。
- 大多数 `server` JSON 接口也返回 `ResultEntity`。
- 文件字节接口可直接返回 `application/octet-stream`。
- 登录后，渲染进程会给 `client-app` 请求加上 `Authorization: Bearer <token>`。
- 当前服务端安全配置允许 `/server/**`，但如果请求携带 Bearer token，JWT filter 会校验 token。业务级权限检查仍是安全模型的一部分。

统一响应包：

```json
{
  "code": 200,
  "message": "OK",
  "time": 1760000000,
  "data": {}
}
```

前端说明：`sync-app/src/renderer/src/utils/request.js` 只解包 HTTP response，不会自动返回 `data.data`。调用方通常拿到的是完整 envelope body，然后读取 `res.data`。

---

## 通用模型

### `ClientConfig`

```json
{
  "serverBaseUrl": "https://example.com",
  "syncHost": "example.com",
  "syncPort": 8080,
  "configured": true
}
```

### `User`

```json
{
  "id": 1,
  "username": "Alice",
  "email": "alice@example.com",
  "avatar": "https://example.com/resources/avatars/1.png?v=1760000000",
  "refreshToken": "jwt-token",
  "accessToken": null
}
```

### `File`

```json
{
  "id": 10,
  "alias": "Work",
  "path": "C:/Users/Alice/Documents",
  "remoteHost": "example.com",
  "scheduled": "1h",
  "cdcAlg": "FastCDC",
  "isDir": true,
  "isSync": false,
  "description": "Work documents",
  "updateTime": "2026-05-16T12:30:00",
  "userId": 1
}
```

### `SubFile`

```json
{
  "id": 101,
  "fileId": 10,
  "parent": null,
  "name": "report.docx",
  "relativePath": "report.docx",
  "depth": 0,
  "isDir": false,
  "isSync": true
}
```

### `Group`

```json
{
  "id": "uuid",
  "name": "Team Docs",
  "ownerEmail": "alice@example.com",
  "admins": ["bob@example.com"],
  "members": ["carol@example.com"],
  "scopes": ["alice@example.com/Work/Documents"]
}
```

### `RemoteScope`

```json
{
  "alias": "Work",
  "rootName": "Documents",
  "isDir": true,
  "scopeName": "alice@example.com/Work/Documents"
}
```

---

## 1. 健康检查

### 1.1 服务端健康检查

```http
GET /
GET /health
```

服务：`server`

响应：

```json
{
  "status": "ok",
  "service": "datasync-server"
}
```

桌面端配置页会用该接口测试远端服务地址。

---

## 2. 客户端运行时配置

这些接口由 `client-app` 提供，登录前也会使用。

### 2.1 获取客户端配置

```http
GET /client/config
```

响应 `data`：`ClientConfig`。

行为：

- 读取 `~/.datasync/client-config.json`。
- 缺失或无效时返回 `{ configured: false, syncPort: 8080 }`。

### 2.2 保存客户端配置

```http
POST /client/config
```

请求体：

| 字段            | 类型   | 必填 | 说明                                                        |
| --------------- | ------ | ---- | ----------------------------------------------------------- |
| `serverBaseUrl` | string | 是   | 中心服务 HTTP(S) base URL。省略 scheme 时自动补 `http://`。 |
| `syncHost`      | string | 否   | 旧版 Netty host。未填时从 `serverBaseUrl` 解析。            |
| `syncPort`      | number | 否   | 旧版 Netty port。默认 `8080`。                              |

响应 `data`：规范化后的 `ClientConfig`。

校验：

- `serverBaseUrl` 必须包含有效 host。
- `syncPort` 必须在 1 到 65535 之间。

### 2.3 测试客户端配置

```http
POST /client/config/test
```

请求体同 [2.2](#22-保存客户端配置)。

响应 `data`：

```json
{
  "serverBaseUrl": "https://example.com",
  "syncHost": "example.com",
  "syncPort": 8080,
  "httpStatus": 200,
  "reachable": true
}
```

行为：

- 规范化配置。
- 向中心服务发送 `GET /`。
- 不可达时返回 `code: 424`。

---

## 3. 认证

认证接口由 `server` 的 `/unauthorized` 暴露，并由 `client-app` 的 `/client/user` 代理使用。

### 3.1 服务端登录

```http
POST /unauthorized/login
```

服务：`server`

请求体：

| 字段       | 类型   | 必填 | 说明                            |
| ---------- | ------ | ---- | ------------------------------- |
| `email`    | string | 是   | 用户邮箱。                      |
| `password` | string | 是   | 明文密码，应通过 HTTP(S) 提交。 |

响应 `data`：

```json
{
  "token": "jwt-token",
  "id": "1",
  "username": "Alice",
  "email": "alice@example.com",
  "avatar": ""
}
```

### 3.2 服务端注册

```http
POST /unauthorized/signup
```

服务：`server`

请求体：

| 字段       | 类型   | 必填 | 说明                            |
| ---------- | ------ | ---- | ------------------------------- |
| `username` | string | 是   | 显示名。                        |
| `email`    | string | 是   | 邮箱，MySQL 中唯一。            |
| `password` | string | 是   | 明文密码，应通过 HTTP(S) 提交。 |

响应 `data`：同登录。

行为：

- 新用户密码以 BCrypt hash 存储。
- 如果邮箱已存在且密码匹配，返回登录数据。
- 如果邮箱已存在但密码不匹配，登录失败。

### 3.3 客户端登录

```http
POST /client/user/login
```

服务：`client-app`

请求体：

```json
{
  "email": "alice@example.com",
  "password": "secret"
}
```

响应 `data`：本地缓存的 `User`。

行为：

- 调用服务端登录。
- 按 email upsert 本地 SQLite `User`。
- 保留服务端 `id` 到本地缓存。

### 3.4 客户端注册

```http
POST /client/user/signup
```

服务：`client-app`

请求体：

```json
{
  "username": "Alice",
  "email": "alice@example.com",
  "password": "secret"
}
```

响应 `data`：本地缓存的 `User`。

---

## 4. 用户接口

### 4.1 通过客户端更新当前用户

```http
POST /client/user/update
```

服务：`client-app`

请求体：`User`。

典型请求：

```json
{
  "id": 1,
  "username": "Alice Chen",
  "email": "alice@example.com",
  "avatar": "data:image/png;base64,...",
  "refreshToken": "jwt-token"
}
```

响应 `data`：更新后的本地缓存 `User`。

行为：

- 转发到 `POST /server/user/update`。
- 将中心服务返回的字段同步到 SQLite。
- `avatar` 可以是已有 URL，也可以是支持的 `data:image/*;base64,...`。

### 4.2 校验本地会话

```http
POST /client/user/session
```

服务：`client-app`

请求体：

| 字段    | 类型   | 必填 | 说明                             |
| ------- | ------ | ---- | -------------------------------- |
| `id`    | string | 否   | localStorage 中的服务端用户 ID。 |
| `email` | string | 是   | localStorage 中的邮箱。          |

响应 `data`：本地缓存 `User`。

本地 SQLite 用户缓存缺失或不匹配时返回 `401`。

### 4.3 恢复最近缓存会话

```http
POST /client/user/session/current
```

服务：`client-app`

请求体：`{}`。

响应 `data`：最近缓存的本地 `User`。

Router 在 Electron localStorage 没有 token、但本地 SQLite 仍有缓存会话时使用该接口。

### 4.4 通过客户端搜索用户

```http
POST /client/user/search
```

服务：`client-app`

请求体：

```json
{ "q": "alice" }
```

响应 `data`：服务端返回的用户摘要数组。

### 4.5 服务端用户搜索

```http
POST /server/user/search
```

服务：`server`

请求体：

```json
{ "q": "alice" }
```

响应 `data`：

```json
[
  {
    "email": "alice@example.com",
    "username": "Alice",
    "avatar": "https://example.com/resources/avatars/1.png?v=1760000000"
  }
]
```

行为：

- 空 query 返回空数组。
- MySQL 模糊搜索逻辑由 `UserMapper.searchByQuery` 实现。

### 4.6 通过邮箱解析用户

```http
POST /server/user/resolve
```

服务：`server`

请求体：

```json
{ "email": "alice@example.com" }
```

响应 `data`：

```json
{
  "id": "1",
  "username": "Alice",
  "email": "alice@example.com",
  "avatar": ""
}
```

用于重装桌面客户端时重建本地身份缓存。

### 4.7 服务端用户更新

```http
POST /server/user/update
```

服务：`server`

请求体：服务端 `User`。

响应 `data`：

```json
{
  "id": "1",
  "username": "Alice Chen",
  "email": "alice@example.com",
  "avatar": "https://example.com/resources/avatars/1.png?v=1760000000"
}
```

校验：

- `id` 必填。
- 如果存在已认证 principal 且其 id 与请求体 id 不一致，返回 `403`。
- 支持的头像 data URL MIME type：`image/png`、`image/jpeg`、`image/webp`、`image/gif`。
- 头像图片必须不超过 2 MB。

---

## 5. 客户端文件树接口

这些接口都是本地 `client-app` 接口，供桌面 UI 调用。

### 5.1 获取同步任务列表

```http
POST /client/file/brief-list
```

请求体：

```json
{ "email": "alice@example.com" }
```

响应 `data`：当前本地用户的 `File[]`。

### 5.2 删除本地文件树条目

```http
POST /client/file/delete
```

请求体：

```json
{
  "email": "alice@example.com",
  "path": "Documents/report.docx"
}
```

响应 `data`：`true`。

该接口删除目标 path 对应的本地 `SubFile` 记录树。它不是删除同步任务；删除任务请使用 `/client/sync/delete`。

### 5.3 获取根级子文件

```http
POST /client/file/detail-list
```

请求体：

```json
{ "fileId": "10" }
```

响应 `data`：`parent` 为 null 的根级 `SubFile[]`。

### 5.4 获取子目录内容

```http
POST /client/file/detail-list-parent
```

请求体：

```json
{
  "fileId": "101",
  "originalId": "10"
}
```

响应 `data`：`parent = fileId` 的 `SubFile[]`。

当前实现只读取 `fileId`；`originalId` 保留用于 UI/路径上下文兼容。

### 5.5 获取远端 scope 用于恢复

```http
POST /client/file/remote-scopes
```

请求体：

```json
{ "email": "alice@example.com" }
```

响应 `data`：`RemoteScope[]`。

行为：

- 转发到 `POST /server/file/list-scopes`。
- 该接口本身不会写本地 SQLite。
- 用于本地桌面端没有任务、但远端服务器已有 scope 的恢复场景。

---

## 6. 客户端同步接口

### 6.1 上行同步

```http
POST /client/sync/upload
```

请求体：

| 字段     | 类型   | 必填                | 说明                                                 |
| -------- | ------ | ------------------- | ---------------------------------------------------- |
| `fileId` | string | 否                  | 优先使用的任务 ID。`null`、空或缺失时回退到 `path`。 |
| `email`  | string | 是                  | 当前用户邮箱。                                       |
| `path`   | string | `fileId` 缺失时必填 | 本地任务根路径。                                     |

响应 `data`：`true`。

行为：

1. 加载当前用户本地任务。
2. 扫描文件并生成 CDC `SyncStyle`。
3. 生成 scope key：`email/alias/rootName`。
4. 调用 `POST /server/file/compare`。
5. 通过 `POST /server/file/upload` 上传文件字节。
6. 将本地任务和子文件标记为已同步。

### 6.2 下行同步

```http
POST /client/sync/download
```

请求体：同上行同步。

响应 `data`：`true`。

行为：

1. 加载本地任务。
2. 生成 scope key：`email/alias/rootName`。
3. 调用 `POST /server/file/download`。
4. 对每个相对路径调用 `POST /server/file/download/file`。
5. 写入本地目标路径，同路径文件会被覆盖。
6. 重建本地 `SubFile` 记录并将任务标记为已同步。

### 6.3 创建或更新同步任务

```http
POST /client/sync/update
```

请求体：

| 字段          | 类型           | 必填 | 说明                                                    |
| ------------- | -------------- | ---- | ------------------------------------------------------- |
| `fileId`      | string         | 否   | 已有 `File.id`；缺失/null/空表示新建任务。              |
| `email`       | string         | 是   | 当前用户邮箱。                                          |
| `alias`       | string         | 是   | 用户内唯一的任务别名。                                  |
| `path`        | string         | 是   | 本地文件或文件夹绝对路径。                              |
| `remoteHost`  | string         | 否   | 元数据/旧版 host 字段。运行时 HTTP URL 来自客户端配置。 |
| `scheduled`   | string         | 否   | 例如 `5m`、`1h`、`1d`、`never`。                        |
| `cdcAlg`      | string         | 是   | `FastCDC`、`FlipCDC`、`QuickCDC` 或 `RabinCDC`。        |
| `description` | string         | 否   | 任务描述。                                              |
| `isDir`       | string/boolean | 是   | `path` 是否为目录。                                     |

响应 `data`：保存后的 `File`。

校验：

- 别名不能为空。
- 路径不能为空。
- 同一用户内别名不能重复。
- 更新已有任务时，该任务必须属于当前本地用户。

### 6.4 删除同步任务

```http
POST /client/sync/delete
```

请求体：

| 字段     | 类型   | 必填                | 说明                |
| -------- | ------ | ------------------- | ------------------- |
| `fileId` | string | 否                  | 优先使用的任务 ID。 |
| `email`  | string | 是                  | 当前用户邮箱。      |
| `path`   | string | `fileId` 缺失时必填 | 本地任务路径。      |

响应 `data`：`true`。

行为：

1. 加载本地任务。
2. 生成 scope key。
3. 调用 `POST /server/group/check-scope`。
4. 如果有群组引用该 scope，返回 `409`。
5. 调用 `POST /server/file/delete-scope`。
6. 删除本地 `SubFile` 和 `File` 记录。

---

## 7. 服务端文件接口

这些接口由 `client-app` 调用。

### 7.1 比对远端 scope

```http
POST /server/file/compare
```

请求体：

| 字段        | 类型          | 必填 | 说明                                         |
| ----------- | ------------- | ---- | -------------------------------------------- |
| `email`     | string        | 是   | 所有者邮箱。                                 |
| `path`      | string        | 是   | 客户端本地根路径。                           |
| `scopeName` | string        | 是   | 完整远端 scope key：`email/alias/rootName`。 |
| `isDir`     | boolean       | 是   | 任务根路径是否为目录。                       |
| `list`      | `SyncStyle[]` | 是   | 客户端文件列表，包含 `storagePath`。         |

响应 `data`：`SyncStyle[]`。

当前行为会清理远端过期文件后返回输入列表。客户端随后通过 HTTP 上传列表中的每个文件。

### 7.2 获取可下载文件列表

```http
POST /server/file/download
```

请求体：

```json
{ "scopeName": "alice@example.com/Work/Documents" }
```

响应 `data`：

```json
["report.docx", "subdir/notes.txt"]
```

行为：

- 返回相对于 scope root 的文件路径。
- 跳过 `.part` 临时文件。
- 单文件 scope 返回文件名。

### 7.3 下载单个文件

```http
POST /server/file/download/file
Content-Type: application/json
Accept: application/octet-stream
```

请求体：

```json
{
  "scopeName": "alice@example.com/Work/Documents",
  "relativePath": "subdir/notes.txt"
}
```

响应：原始文件字节，`Content-Type: application/octet-stream`。

会拒绝跳出 `scopeName` 的路径穿越。

### 7.4 上传单个文件

```http
POST /server/file/upload?storagePath=<path>&fileName=<name>
Content-Type: application/octet-stream
```

Query 参数：

| 字段          | 类型   | 必填 | 说明                                                                           |
| ------------- | ------ | ---- | ------------------------------------------------------------------------------ |
| `storagePath` | string | 是   | 相对于 `basePath` 的远端目录，例如 `alice@example.com/Work/Documents/subdir`。 |
| `fileName`    | string | 是   | 仅文件名。路径分隔符、`.`、`..` 会被拒绝。                                     |

请求体：原始文件字节。

响应 `data`：

```json
{ "bytes": 12345 }
```

行为：

- 先写 `<fileName>.part`。
- 完成后替换最终文件。
- 目标路径必须位于配置的 `basePath` 内。

### 7.5 删除远端 scope

```http
POST /server/file/delete-scope
```

请求体：

```json
{ "scopeName": "alice@example.com/Work/Documents" }
```

响应 `data`：`true`。

删除该 scope 对应的文件或文件夹，并向上清理空父目录直到 `basePath`。

### 7.6 获取用户远端 scope

```http
POST /server/file/list-scopes
```

请求体：

```json
{ "email": "alice@example.com" }
```

响应 `data`：`RemoteScope[]`。

扫描 `basePath/<email>/` 并返回可恢复的任务 scope。

### 7.7 迁移旧存储布局

```http
POST /server/file/migrate-legacy-storage
```

请求体：`{}`。

响应 `data`：

```json
{
  "scanned": 10,
  "migrated": 8,
  "skipped": 2,
  "failed": 0,
  "groupScopes": 3
}
```

仅在 `ScopeStorageMigrationRunner` 激活时可用。非 docker profile 返回 `409`。

---

## 8. 客户端群组接口

除 `download-scope` 外，以下 `/client/group` 接口都会由 `client-app` 转发到中心服务端。`download-scope` 会在本地启动后台下载任务。

### 8.1 创建群组

```http
POST /client/group/create
```

请求：

```json
{ "email": "alice@example.com", "name": "Team Docs" }
```

响应 `data`：`Group`。

### 8.2 添加成员

```http
POST /client/group/add-member
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmail": "carol@example.com"
}
```

响应 `data`：更新后的 `Group`。

### 8.3 移除成员

```http
POST /client/group/remove-member
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmail": "carol@example.com"
}
```

响应 `data`：更新后的 `Group`。

### 8.4 添加共享 scope

```http
POST /client/group/add-scope
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "scopeName": "alice@example.com/Work/Documents"
}
```

响应 `data`：更新后的 `Group`。

### 8.5 移除共享 scope

```http
POST /client/group/remove-scope
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "scopeName": "alice@example.com/Work/Documents"
}
```

响应 `data`：更新后的 `Group`。

### 8.6 删除群组

```http
POST /client/group/delete
```

请求：

```json
{ "email": "alice@example.com", "groupId": "uuid" }
```

响应 `data`：`true`。

仅 owner 可执行。

### 8.7 获取群组列表

```http
POST /client/group/list
```

请求：

```json
{ "email": "alice@example.com" }
```

响应 `data`：当前用户作为 owner、admin 或 member 参与的群组。

### 8.8 获取群组文件结构

```http
POST /client/group/files
```

请求：

```json
{ "email": "alice@example.com" }
```

响应 `data`：

```json
[
  {
    "id": "uuid",
    "name": "Team Docs",
    "ownerEmail": "alice@example.com",
    "owner": true,
    "admins": ["bob@example.com"],
    "members": ["carol@example.com"],
    "scopes": [
      {
        "scopeName": "alice@example.com/Work/Documents",
        "alias": "Work",
        "rootName": "Documents",
        "displayName": "Documents",
        "files": [
          { "name": "report.docx", "relativePath": "report.docx", "dir": false }
        ]
      }
    ]
  }
]
```

### 8.9 添加管理员

```http
POST /client/group/add-admin
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "adminEmail": "bob@example.com"
}
```

响应 `data`：更新后的 `Group`。

仅 owner 可执行。

### 8.10 移除管理员

```http
POST /client/group/remove-admin
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "adminEmail": "bob@example.com"
}
```

响应 `data`：更新后的 `Group`。

仅 owner 可执行。移除管理员时会在适当情况下将其降级为普通成员。

### 8.11 批量添加成员

```http
POST /client/group/add-members
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmails": ["carol@example.com", "dave@example.com"]
}
```

响应 `data`：更新后的 `Group`。

### 8.12 批量移除成员

```http
POST /client/group/remove-members
```

请求：

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmails": ["carol@example.com", "dave@example.com"]
}
```

响应 `data`：更新后的 `Group`。

### 8.13 下载群组 scope

```http
POST /client/group/download-scope
```

请求：

| 字段           | 类型   | 必填 | 说明                                               |
| -------------- | ------ | ---- | -------------------------------------------------- |
| `email`        | string | 否   | 当前用户邮箱；当前实现未使用。                     |
| `scopeName`    | string | 是   | 完整远端 scope key。                               |
| `localPath`    | string | 是   | 本地目标目录。                                     |
| `relativePath` | string | 否   | scope 内可选文件或文件夹路径。空值下载整个 scope。 |

响应 `data`：`null`。

行为：

- 立即返回。
- 后台下载 scope 文件到 `localPath`。
- 如果 `relativePath` 指向文件，只下载该文件。
- 如果 `relativePath` 指向文件夹前缀，下载该前缀下匹配文件。

---

## 9. 服务端群组接口

服务端群组接口与客户端转发接口对应：

```http
POST /server/group/create
POST /server/group/add-member
POST /server/group/remove-member
POST /server/group/add-scope
POST /server/group/remove-scope
POST /server/group/delete
POST /server/group/list
POST /server/group/files
POST /server/group/add-admin
POST /server/group/remove-admin
POST /server/group/add-members
POST /server/group/remove-members
POST /server/group/check-scope
```

### 9.1 角色权限

| 操作                | 所需角色                                      |
| ------------------- | --------------------------------------------- |
| 创建群组            | 任意调用邮箱都可创建自己的群组。              |
| 删除群组            | Owner                                         |
| 添加/移除 admin     | Owner                                         |
| 添加/移除普通成员   | Owner 或 admin                                |
| 添加/移除 scope     | Owner 或 admin                                |
| 查看群组/文件       | Owner、admin 或 member 可查看自己参与的群组。 |
| 检查 scope 是否可删 | 无角色检查；供客户端删除保护使用。            |

### 9.2 检查 scope 是否可删除

```http
POST /server/group/check-scope
```

请求：

```json
{
  "email": "alice@example.com",
  "scopeName": "alice@example.com/Work/Documents"
}
```

响应 `data`：如果没有任何群组引用该 scope，则为 `true`；否则为 `false`。

---

## 10. 客户端日志

### 10.1 读取本地客户端日志尾部

```http
GET /client/log/list?lines=300
```

服务：`client-app`

Query 参数：

| 字段    | 类型   | 默认值 | 说明             |
| ------- | ------ | ------ | ---------------- |
| `lines` | number | `300`  | 返回末尾多少行。 |

响应 `data`：`string[]`。

读取路径：

```text
log/log.log
```

该路径相对于 `client-app` 工作目录。

---

## 11. 错误处理

大多数错误使用 HTTP 200 加 envelope 内非 2xx `code` 的形式返回：

```json
{
  "code": 409,
  "message": "Task name already exists",
  "time": 1760000000,
  "data": null
}
```

部分原始字节接口或底层 HTTP 失败可能使用非 2xx HTTP 状态码。`HttpJsonClient` 会同时把非 2xx HTTP status 和非 2xx envelope `code` 当作失败。

常见 code：

|  Code | 含义                                     |
| ----: | ---------------------------------------- |
| `400` | 请求数据缺失或非法。                     |
| `401` | 登录、会话或 token 失败。                |
| `403` | 角色或资料归属权限不足。                 |
| `404` | 目标用户、群组或资源不存在。             |
| `409` | 业务冲突，例如任务别名重复、受保护删除。 |
| `424` | 远端服务器连接测试失败。                 |
| `500` | 服务端或本地代理未预期异常。             |
