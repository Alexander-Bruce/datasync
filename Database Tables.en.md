# Database Tables

**[English](Database%20Tables.en.md) | [中文](Database%20Tables.md)**

---

### Server

#### User

| Column   | Type   | Description                    |
| -------- | ------ | ------------------------------ |
| id       | int    | User ID (primary key)          |
| username | string | Display name                   |
| email    | string | Email address                  |
| password | string | BCrypt-hashed password         |
| phone    | string | Phone number                   |
| avatar   | string | Avatar URL                     |

> Entity class: `backend.model.entity.User` (implements `UserDetails`)

---

### Client

#### User

| Column        | Type   | Description                  |
| ------------- | ------ | ---------------------------- |
| username      | string | Display name                 |
| email         | string | Email address                |
| ip            | string | User IP address              |
| user_agent    | string | User agent string            |
| refresh_token | string | JWT Refresh Token            |
| access_token  | string | JWT Access Token             |

> Stored in local SQLite. Entity class: `backend.model.entity.User`

---

#### File

| Column      | Type    | Description                                                              |
| ----------- | ------- | ------------------------------------------------------------------------ |
| id          | int     | Primary key (auto-increment)                                             |
| alias       | string  | Sync task alias                                                          |
| description | string  | Sync task description                                                    |
| path        | string  | Absolute local path                                                      |
| remote_host | string  | Remote server address (host:port)                                        |
| scheduled   | string  | Schedule policy (Cron expression, e.g. `0 9 * * *`)                     |
| cdc_alg     | string  | CDC algorithm class name (FastCDC / FlipCDC / QuickCDC / RabinCDC)      |
| is_dir      | boolean | Whether this entry is a directory                                        |
| is_sync     | boolean | Whether this entry has been synced                                       |
| update_time | string  | Last update time (ISO format string)                                     |
| user_id     | int     | Foreign key referencing the client User table                            |

> Entity class: `backend.model.entity.File`

---

#### SubFile

| Column        | Type    | Description                                               |
| ------------- | ------- | --------------------------------------------------------- |
| id            | int     | Primary key (auto-increment)                              |
| file_id       | int     | Foreign key referencing the File table                    |
| parent        | int     | Parent SubFile id (null for root-level entries)           |
| name          | string  | File / directory name                                     |
| relative_path | string  | Path relative to File.path                               |
| depth         | int     | Tree depth relative to the sync root (root children = 0) |
| is_dir        | boolean | Whether this entry is a directory                         |
| is_sync       | boolean | Whether this entry has been synced                        |

> Entity class: `backend.model.entity.SubFile`
