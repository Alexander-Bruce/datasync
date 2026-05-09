# Database Tables

**[English](Database%20Tables.en.md) | [中文](Database%20Tables.md)**

---

### Server

#### User

| Column     | Type    | Description          |
| ---------- | ------- | -------------------- |
| id         | int     | 用户 id（主键）      |
| username   | string  | 用户名称             |
| email      | string  | 电子邮件             |
| password   | string  | BCrypt 加密后密码    |
| phone      | string  | 手机号               |
| avatar     | string  | 头像 URL             |

> 对应实体类：`backend.model.entity.User`（实现 `UserDetails`）

---

### Client

#### User

| Column        | Type   | Description      |
| ------------- | ------ | ---------------- |
| username      | string | 用户名称         |
| email         | string | 电子邮件         |
| ip            | string | 用户 IP 地址     |
| user_agent    | string | 用户访问方式     |
| refresh_token | string | JWT Refresh Token |
| access_token  | string | JWT Access Token  |

> 存储于本地 SQLite，对应实体类：`backend.model.entity.User`

---

#### File

| Column      | Type    | Description                                   |
| ----------- | ------- | --------------------------------------------- |
| id          | int     | 主键（自增）                                  |
| alias       | string  | 同步任务别名                                  |
| description | string  | 同步任务描述                                  |
| path        | string  | 本地绝对路径                                  |
| remote_host | string  | 远端服务器地址（host:port）                   |
| scheduled   | string  | 定时策略（Cron 表达式，如 `0 9 * * *`）       |
| cdc_alg     | string  | CDC 算法类名（FastCDC / FlipCDC / QuickCDC / RabinCDC） |
| is_dir      | boolean | 是否为文件夹                                  |
| is_sync     | boolean | 是否已同步                                    |
| update_time | string  | 最后更新时间（ISO 格式字符串）                |
| user_id     | int     | 关联客户端 User 表                            |

> 对应实体类：`backend.model.entity.File`

---

#### SubFile

| Column        | Type    | Description                          |
| ------------- | ------- | ------------------------------------ |
| id            | int     | 主键（自增）                         |
| file_id       | int     | 关联 File 表主键                     |
| parent        | int     | 父节点 SubFile id（根节点为 null）   |
| name          | string  | 文件 / 目录名称                      |
| relative_path | string  | 相对于 File.path 的路径              |
| depth         | int     | 相对于同步根的树深度（根子级为 0）   |
| is_dir        | boolean | 是否为文件夹                         |
| is_sync       | boolean | 是否已同步                           |

> 对应实体类：`backend.model.entity.SubFile`
