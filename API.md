# DataSync API 文档

**[English](API.en.md) | [中文](API.md)**

> 基础约定：
> - **Server**（Spring Boot）运行在 `http://localhost:8090`
> - **Client**（Spring Boot）运行在 `http://localhost:8092`
> - 所有请求体 / 响应体均为 JSON
> - 鉴权接口以外，请求头需携带 `Authorization: Bearer <access_token>`
> - 统一响应包装结构：
>
> ```json
> {
>   "code": 200,
>   "message": "...",
>   "data": { ... }
> }
> ```

---

## 一、认证接口（Server, `/unauthorized`，无需鉴权）

### 1.1 登录

```
POST /unauthorized/login
```

**请求体**

| 字段     | 类型   | 说明     |
| -------- | ------ | -------- |
| email    | string | 电子邮件 |
| password | string | 明文密码 |

**响应 data**

| 字段          | 类型   | 说明                |
| ------------- | ------ | ------------------- |
| access_token  | string | JWT Access Token    |
| refresh_token | string | JWT Refresh Token   |

---

### 1.2 注册

```
POST /unauthorized/signup
```

**请求体**

| 字段     | 类型   | 说明     |
| -------- | ------ | -------- |
| username | string | 用户名   |
| email    | string | 电子邮件 |
| password | string | 明文密码 |

**响应 data**：注册成功的用户对象

---

### 1.3 刷新 Access Token

```
GET /unauthorized/refesh-token
```

**Query 参数**

| 参数 | 类型   | 说明           |
| ---- | ------ | -------------- |
| id   | string | 用户 id        |

**响应 data**：新的 `access_token`

---

## 二、客户端用户接口（Client, `/client/user`）

### 2.1 登录（客户端本地）

```
POST /client/user/login
```

**请求体**

| 字段     | 类型   | 说明     |
| -------- | ------ | -------- |
| email    | string | 电子邮件 |
| password | string | 明文密码 |

---

### 2.2 注册（客户端本地）

```
POST /client/user/signup
```

**请求体**

| 字段     | 类型   | 说明     |
| -------- | ------ | -------- |
| username | string | 用户名   |
| email    | string | 电子邮件 |
| password | string | 明文密码 |

---

## 三、文件列表接口（Client, `/client/file`）

### 3.1 获取同步任务列表（Brief List）

```
POST /client/file/brief-list
```

**请求体**

| 字段  | 类型   | 说明       |
| ----- | ------ | ---------- |
| email | string | 当前用户邮箱 |

**响应 data**：`File[]`，每项字段见 [Database Tables.md](./Database%20Tables.md) `Client.File`

---

### 3.2 获取目录根级子文件列表（Detail List）

```
POST /client/file/detail-list
```

**请求体**

| 字段   | 类型   | 说明            |
| ------ | ------ | --------------- |
| fileId | string | File 表主键 id  |

**响应 data**：`SubFile[]`，`parent` 为 null 的根子级列表

---

### 3.3 获取子目录的子文件列表

```
POST /client/file/detail-list-parent
```

**请求体**

| 字段       | 类型   | 说明                              |
| ---------- | ------ | --------------------------------- |
| fileId     | string | 目标文件夹的 SubFile id           |
| originalId | string | 根 File 表 id（用于路径校验）     |

**响应 data**：`SubFile[]`，指定文件夹下的直接子项列表

---

## 四、同步操作接口（Client, `/client/sync`）

### 4.1 上行同步（Upload）

将本地文件同步至服务端，完成后更新本地 `is_sync = true`。

```
POST /client/sync/upload
```

**请求体**

| 字段  | 类型   | 说明                         |
| ----- | ------ | ---------------------------- |
| email | string | 当前用户邮箱                 |
| path  | string | 同步任务本地根路径（绝对路径） |

**响应 data**：`true` / `false`

**流程说明**
1. 根据 `path` 和 `email` 查找本地 SQLite `File` 记录，取得 CDC 算法类名
2. 对本地文件做 CDC 分块；每条 `SyncStyle` 的 `storagePath` 以 `email/folderName/...` 格式构建（如 `alice@example.com/Documents/subdir`），确保多用户间同名文件夹不冲突
3. Client 调用 `POST /server/file/compare`（携带 `email`），Server 将文件写入 `basePath/email/folderName/`
4. 通过 Netty 将差量块（加密）传输到 Server，Server 按 `storagePath` 写入对应子目录
5. 同步成功后更新本地 `File.is_sync` 及所有 `SubFile.is_sync` 为 `true`

---

### 4.2 下行同步（Download）

将服务端存储的文件拉取到本地并直接覆盖。

```
POST /client/sync/download
```

**请求体**

| 字段  | 类型   | 说明                         |
| ----- | ------ | ---------------------------- |
| email | string | 当前用户邮箱                 |
| path  | string | 同步任务本地根路径（绝对路径） |

**响应 data**：`true` / `false`

**流程说明**
1. 根据 `path` 查找本地 `File` 记录；`scopeName = email + "/" + 路径末段`（如 `alice@example.com/Documents`）
2. 调用 `POST /server/file/download`（传入 `scopeName`），Server 扫描 `basePath/email/folderName/`
3. 逐文件写入本地 `path/relativePath`，覆盖已有文件
4. 更新本地 `File.is_sync` 及 `SubFile.is_sync` 为 `true`

---

### 4.3 创建 / 更新同步任务

```
POST /client/sync/update
```

**请求体**

| 字段        | 类型   | 说明                        |
| ----------- | ------ | --------------------------- |
| fileId      | string | File 表 id（新建时传 null） |
| email       | string | 当前用户邮箱                |
| alias       | string | 任务别名                    |
| path        | string | 本地绝对路径                |
| remoteHost  | string | 远端主机地址                |
| scheduled   | string | Cron 表达式（可选）         |
| cdcAlg      | string | CDC 算法名称                |
| description | string | 任务描述（可选）            |

**响应 data**：更新后的 `File` 对象

---

### 4.4 删除同步任务

```
POST /client/sync/delete
```

**请求体**

| 字段  | 类型   | 说明             |
| ----- | ------ | ---------------- |
| email | string | 当前用户邮箱     |
| path  | string | 要删除任务的路径 |

**响应 data**：`true` / `false`

**前置检查**：删除前 Client 调用 `POST /server/group/check-scope` 验证该任务的 scope（`email/folderName`）是否仍被某个有成员的群组引用。若是，返回 `code: 409` 并携带错误消息，删除操作不执行，前端在确认弹窗中展示错误提示。用户须先删除相关群组才能删除任务。

---

## 五、服务端文件操作接口（Server, `/server/file`）

> 这些接口由 Client 内部调用，不直接暴露给前端。

### 5.1 差量比对（Compare）

```
POST /server/file/compare
```

**请求体**

| 字段  | 类型         | 说明                                                       |
| ----- | ------------ | ---------------------------------------------------------- |
| email | string       | 当前用户邮箱（用于构造 `email/folderName` 存储路径）       |
| path  | string       | 客户端本地根路径（用于推导服务端范围目录末段）             |
| list  | SyncStyle[]  | 客户端文件分块信息列表（`storagePath` 已含 email 前缀）    |

**服务端范围目录**：`basePath/email/folderName/`（例如 `C:\Sync\alice@example.com\Documents\`），保证多用户同名文件夹隔离。

**响应 data**：`SyncStyle[]`，需要传输的文件列表（服务端缺失 / 变更的文件）

**SyncStyle 字段**

| 字段        | 类型   | 说明                                                          |
| ----------- | ------ | ------------------------------------------------------------- |
| file        | File   | 文件对象（含绝对路径）                                        |
| syncType    | enum   | FastCDC / FlipCDC / RabinCDC / QuickCDC                       |
| storagePath | string | 服务端存储的相对路径（格式：`email/folderName` 或 `email/folderName/subdir`） |

---

### 5.2 下行同步文件列表（Download）

```
POST /server/file/download
```

**请求体**

| 字段      | 类型   | 说明                           |
| --------- | ------ | ------------------------------ |
| scopeName | string | 同步范围路径，格式为 `email/folderName`（如 `alice@example.com/Documents`） |

**响应 data**：服务端该 scope 目录下所有文件的相对路径列表 `string[]`（不含内容）。

内容通过 `POST /server/file/download/file` 逐文件获取。

---

## 六、群组管理接口（Client, `/client/group`）

> 这些接口由前端调用，Client 内部不做存储，直接转发至 Server。

### 6.1 创建群组

```
POST /client/group/create
```

**请求体**

| 字段  | 类型   | 说明         |
| ----- | ------ | ------------ |
| email | string | 当前用户邮箱 |
| name  | string | 群组名称     |

**响应 data**：新建的 `Group` 对象

---

### 6.2 添加成员

```
POST /client/group/add-member
```

**请求体**

| 字段        | 类型   | 说明           |
| ----------- | ------ | -------------- |
| email       | string | 当前用户邮箱   |
| groupId     | string | 群组 id        |
| memberEmail | string | 被添加成员邮箱 |

**响应 data**：更新后的 `Group` 对象

---

### 6.3 移除成员

```
POST /client/group/remove-member
```

**请求体**

| 字段        | 类型   | 说明           |
| ----------- | ------ | -------------- |
| email       | string | 当前用户邮箱   |
| groupId     | string | 群组 id        |
| memberEmail | string | 被移除成员邮箱 |

**响应 data**：更新后的 `Group` 对象

---

### 6.4 添加共享文件夹

```
POST /client/group/add-scope
```

**请求体**

| 字段      | 类型   | 说明                                        |
| --------- | ------ | ------------------------------------------- |
| email     | string | 当前用户邮箱                                |
| groupId   | string | 群组 id                                     |
| scopeName | string | 同步范围键，格式 `ownerEmail/folderName`（如 `alice@example.com/Work`）；前端由当前用户邮箱 + 选定任务路径末段自动构建 |

**响应 data**：更新后的 `Group` 对象

---

### 6.5 移除共享文件夹

```
POST /client/group/remove-scope
```

**请求体**

| 字段      | 类型   | 说明           |
| --------- | ------ | -------------- |
| email     | string | 当前用户邮箱   |
| groupId   | string | 群组 id        |
| scopeName | string | 同步范围名称   |

**响应 data**：更新后的 `Group` 对象

---

### 6.6 删除群组

```
POST /client/group/delete
```

**请求体**

| 字段    | 类型   | 说明         |
| ------- | ------ | ------------ |
| email   | string | 当前用户邮箱 |
| groupId | string | 群组 id      |

**响应 data**：`true`

---

### 6.7 获取群组列表

```
POST /client/group/list
```

**请求体**

| 字段  | 类型   | 说明         |
| ----- | ------ | ------------ |
| email | string | 当前用户邮箱 |

**响应 data**：`Group[]`，包含当前用户创建或加入的所有群组

**Group 字段**

| 字段       | 类型     | 说明             |
| ---------- | -------- | ---------------- |
| id         | string   | 群组 UUID        |
| name       | string   | 群组名称         |
| ownerEmail | string   | 创建者邮箱       |
| members    | string[] | 成员邮箱列表     |
| scopes     | string[] | 共享文件夹键列表，每项格式为 `ownerEmail/folderName` |

---

### 6.8 获取群组文件结构

```
POST /client/group/files
```

**请求体**

| 字段  | 类型   | 说明         |
| ----- | ------ | ------------ |
| email | string | 当前用户邮箱 |

**响应 data**：`GroupInfo[]`，包含每个群组下所有共享范围的文件树

**GroupInfo 字段**

| 字段       | 类型            | 说明                         |
| ---------- | --------------- | ---------------------------- |
| id         | string          | 群组 UUID                    |
| name       | string          | 群组名称                     |
| ownerEmail | string          | 创建者邮箱                   |
| owner      | boolean         | 当前用户是否为群组创建者     |
| members    | string[]        | 成员邮箱列表                 |
| scopes     | GroupScopeInfo[] | 共享文件夹及其文件列表       |

**GroupScopeInfo 字段**

| 字段      | 类型            | 说明               |
| --------- | --------------- | ------------------ |
| scopeName | string          | 文件夹键（`ownerEmail/folderName`）；前端展示时截取 `/` 后的部分作为显示名 |
| files     | GroupFileNode[] | 该范围下的文件列表（扁平化，含所有层级） |

**GroupFileNode 字段**

| 字段         | 类型    | 说明                           |
| ------------ | ------- | ------------------------------ |
| name         | string  | 文件 / 文件夹名称              |
| relativePath | string  | 相对于 scope 根目录的路径      |
| dir          | boolean | 是否为文件夹                   |

---

### 6.9 异步下行同步（群组 Scope）

```
POST /client/group/download-scope
```

立即返回，后台异步将服务端 scope 目录下全部文件下载到指定本地路径。

**请求体**

| 字段      | 类型   | 说明                   |
| --------- | ------ | ---------------------- |
| email     | string | 当前用户邮箱           |
| scopeName | string | 要下载的同步范围名称   |
| localPath | string | 本地保存的根目录路径   |

**响应 data**：`null`（任务已在后台启动）

---

## 七、群组管理接口（Server, `/server/group`）

> 这些接口由 Client 内部调用，不直接暴露给前端。群组数据以 JSON 文件形式存储于 `basePath/groups.json`。

### 7.1 创建群组

```
POST /server/group/create
```

**请求体**：`{ email, name }`  
**响应 data**：新建的 `Group` 对象

---

### 7.2 添加成员

```
POST /server/group/add-member
```

**请求体**：`{ email, groupId, memberEmail }`  
**响应 data**：更新后的 `Group` 对象

---

### 7.3 移除成员

```
POST /server/group/remove-member
```

**请求体**：`{ email, groupId, memberEmail }`  
**响应 data**：更新后的 `Group` 对象

---

### 7.4 添加共享文件夹

```
POST /server/group/add-scope
```

**请求体**：`{ email, groupId, scopeName }`  
**响应 data**：更新后的 `Group` 对象

---

### 7.5 移除共享文件夹

```
POST /server/group/remove-scope
```

**请求体**：`{ email, groupId, scopeName }`  
**响应 data**：更新后的 `Group` 对象

---

### 7.6 删除群组

```
POST /server/group/delete
```

**请求体**：`{ email, groupId }`  
**响应 data**：`true`

---

### 7.7 获取群组列表

```
POST /server/group/list
```

**请求体**：`{ email }`  
**响应 data**：`Group[]`

---

### 7.8 获取群组文件结构

```
POST /server/group/files
```

**请求体**：`{ email }`  
**响应 data**：`GroupInfo[]`，遍历 `basePath/email/folderName/` 目录构建文件树

---

### 7.9 检查 Scope 是否可删除

```
POST /server/group/check-scope
```

**请求体**

| 字段      | 类型   | 说明                                                  |
| --------- | ------ | ----------------------------------------------------- |
| email     | string | 当前用户邮箱（仅用于日志，校验逻辑以 scopeName 为准） |
| scopeName | string | 待检查的 scope 键（`email/folderName`）               |

**响应 data**：`true`（可删除）/ `false`（有群组仍在引用且群组有成员）

**说明**：Client 在执行 `POST /client/sync/delete` 前调用此接口。若返回 `false`，Client 抛出 `BaseException(409)` 并携带提示消息，前端在删除确认弹窗中展示错误。

---

## 八、Netty 同步协议（上行，内部通信）

> 监听端口由配置 `spring.netty.server.port` 决定（默认 8888）。

**握手包 `StartSyncRequest`**

| 字段            | 类型   | 说明                                       |
| --------------- | ------ | ------------------------------------------ |
| fileName        | string | 文件名                                     |
| storagePath     | string | 服务端存储相对路径                         |
| cdcClassName    | string | CDC 实现类全限定名                         |
| EncryptedAESKey | byte[] | RSA-OAEP-SHA256 加密后的 AES-256 密钥      |

**数据包 `SyncPacket`**

| 字段 | 类型        | 说明                                              |
| ---- | ----------- | ------------------------------------------------- |
| type | enum        | `REFERENCE`（引用已有块）/ `DATA`（新块）/ `EOF` |
| hash | string      | 块 SHA-256 哈希（REFERENCE 类型使用）             |
| data | byte[]      | AES-GCM 加密后的块数据（DATA 类型使用）           |

**加密方案**

- 数据加密：AES-256-GCM，随机 12 字节 IV，每个 `SyncPacket` 独立加密
- 密钥交换：RSA-2048 OAEP-SHA256，公钥存于 `conf/rsa-public.pem`
