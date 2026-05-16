# DataSync Database And Storage Reference

**English | [中文](Database%20Tables.md)**

DataSync uses three storage layers:

1. MySQL/MariaDB on the central `server`.
2. SQLite on the local `client-app`.
3. JSON/filesystem storage under the server `basePath`.

---

## 1. Server MySQL / MariaDB

Initialization script:

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

| Column       | Type                 | Null | Description                                                              |
| ------------ | -------------------- | ---- | ------------------------------------------------------------------------ |
| `id`         | `INT AUTO_INCREMENT` | no   | Server user ID. This value must be mirrored into local SQLite `User.id`. |
| `username`   | `VARCHAR(191)`       | no   | Display name.                                                            |
| `email`      | `VARCHAR(191)`       | no   | Unique email. Used as the primary identity key across sync/group APIs.   |
| `password`   | `VARCHAR(255)`       | no   | BCrypt-hashed password.                                                  |
| `avatar`     | `TEXT`               | yes  | Avatar URL or empty value. Large image bytes should not be stored here.  |
| `created_at` | `TIMESTAMP`          | no   | Row creation time.                                                       |
| `updated_at` | `TIMESTAMP`          | no   | Last row update time.                                                    |

Indexes:

| Index               | Columns    | Purpose                                                          |
| ------------------- | ---------- | ---------------------------------------------------------------- |
| `PRIMARY KEY`       | `id`       | User lookup by numeric ID and JWT subject.                       |
| `uk_user_email`     | `email`    | Prevent duplicate accounts; group membership uses email strings. |
| `idx_user_username` | `username` | Supports username-oriented search.                               |

Mapped by:

```text
server/src/main/java/backend/model/entity/User.java
server/src/main/java/backend/mapper/mysql/UserMapper.java
server/src/main/resources/backend/mapper/mysql/UserMapper.xml
```

Notes:

- Passwords are created with BCrypt strength 12.
- `searchByQuery` supports fuzzy lookup for group member search/autocomplete.
- `avatar` should be a remotely loadable URL. When users upload base64 avatars, the server writes the file to `basePath/avatars` and stores the public URL string.

---

## 2. Client SQLite

Schema script:

```text
client-app/src/main/resources/backend/mapper/sqlite/scheme.sql
```

Default local database:

```text
datasync-user.db
```

Configured by:

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

| Column          | Type      | Null                                | Description                                       |
| --------------- | --------- | ----------------------------------- | ------------------------------------------------- |
| `id`            | `INTEGER` | yes in schema, required in practice | Server user ID. Required by task operations.      |
| `username`      | `TEXT`    | no                                  | Display name. Current primary key.                |
| `email`         | `TEXT`    | no                                  | Unique email. Used for local lookup/upsert.       |
| `avatar`        | `TEXT`    | yes                                 | Avatar URL or empty string.                       |
| `ip`            | `TEXT`    | yes                                 | Reserved/local metadata field.                    |
| `user_agent`    | `TEXT`    | yes                                 | Reserved/local metadata field.                    |
| `refresh_token` | `TEXT`    | yes                                 | Cached JWT token returned by server login/signup. |
| `access_token`  | `TEXT`    | yes                                 | Reserved token field.                             |

Important invariant:

- `User.email` is the local unique identity key used by service code.
- `User.id` must be populated with the central server ID. If it is null, `File.user_id` cannot be resolved correctly.

Mapped by:

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

| Column        | Type                                | Null | Description                                                                                  |
| ------------- | ----------------------------------- | ---- | -------------------------------------------------------------------------------------------- |
| `id`          | `INTEGER PRIMARY KEY AUTOINCREMENT` | no   | Local sync task ID.                                                                          |
| `alias`       | `TEXT`                              | yes  | User-facing task name. Must be unique per local user in service logic.                       |
| `path`        | `TEXT`                              | yes  | Absolute local path to the task root file/folder.                                            |
| `remote_host` | `TEXT`                              | yes  | Metadata/legacy host field. Runtime HTTP URL comes from client config.                       |
| `scheduled`   | `TEXT`                              | yes  | Upload interval: `5m`, `1h`, `1d`, a minute number, empty, or `never`.                       |
| `cdc_alg`     | `TEXT`                              | yes  | One of `FastCDC`, `FlipCDC`, `QuickCDC`, `RabinCDC`.                                         |
| `is_dir`      | `BOOLEAN`                           | no   | Whether `path` is a directory task.                                                          |
| `is_sync`     | `BOOLEAN`                           | no   | Whether local state is considered synced.                                                    |
| `description` | `TEXT`                              | yes  | User task description.                                                                       |
| `update_time` | `TEXT`                              | yes  | Last task update time. Java ISO and SQLite datetime strings are both parsed by watcher code. |
| `user_id`     | `INTEGER`                           | yes  | Server user ID from local `User.id`.                                                         |

Service-level behavior:

- `updateFileTask()` validates non-empty alias/path.
- Duplicate alias for the same user returns a conflict.
- Path changes trigger a post-commit rescan of `SubFile`.
- Upload/download can target by `fileId`; fallback path lookup exists for compatibility.

Mapped by:

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

| Column          | Type                                | Null | Description                                                         |
| --------------- | ----------------------------------- | ---- | ------------------------------------------------------------------- |
| `id`            | `INTEGER PRIMARY KEY AUTOINCREMENT` | no   | Local file-tree row ID.                                             |
| `file_id`       | `INTEGER`                           | no   | Parent sync task ID.                                                |
| `parent`        | `INTEGER`                           | yes  | Parent `SubFile.id`; null for root-level children.                  |
| `name`          | `TEXT`                              | yes  | File or folder name.                                                |
| `relative_path` | `TEXT`                              | yes  | Path relative to `File.path`, using `/` separators in service code. |
| `depth`         | `INTEGER`                           | yes  | Tree depth relative to the task root. Root children are depth 0.    |
| `is_dir`        | `BOOLEAN`                           | no   | Whether this row represents a directory.                            |
| `is_sync`       | `BOOLEAN`                           | no   | Whether this row is considered synced.                              |

Service-level behavior:

- `addFiles()` scans with BFS so parent directories are inserted before children.
- Directory tasks store root children as `SubFile` rows.
- Single-file tasks also create one `SubFile` row for the file itself.
- `saveSubFileListInTransaction()` rebuilds rows for a task.
- `refreshSubFiles()` inserts missing disk rows without deleting existing rows.

Mapped by:

```text
client-app/src/main/java/backend/model/entity/SubFile.java
client-app/src/main/java/backend/mapper/sqlite/SubFileMapper.java
client-app/src/main/resources/backend/mapper/sqlite/SubFileMapper.xml
```

---

## 3. Server Filesystem Storage

Configured by:

```yaml
application:
  netty:
    server:
      basePath: /sync
```

Layout:

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

Canonical scope key:

```text
<email>/<alias>/<rootName>
```

Examples:

```text
alice@example.com/Work/Documents
alice@example.com/SingleFile/budget.xlsx
```

The scope key is shared by:

- Upload compare.
- Download list/content.
- Group shared scopes.
- Task deletion guard.
- Remote task restore.

Never replace this with display-only names such as `Documents`, or with the older `email/rootName` layout.

### 3.2 Upload Storage Path

During upload, the client sends:

```text
storagePath = <email>/<alias>/<relative directory>
fileName = <file name only>
```

The server writes:

```text
basePath/storagePath/fileName
```

The server rejects:

- Absolute paths.
- Empty segments.
- `.` or `..`.
- Null bytes.
- File names containing `/` or `\`.

### 3.3 Temporary Upload Files

Upload writes to:

```text
<fileName>.part
```

Then replaces:

```text
<fileName>
```

Download/list operations skip `.part` files.

---

## 4. `groups.json`

Stored at:

```text
<basePath>/groups.json
```

Shape:

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

| Field        | Type     | Description                                                 |
| ------------ | -------- | ----------------------------------------------------------- |
| `id`         | string   | UUID generated when creating the group.                     |
| `name`       | string   | Group name.                                                 |
| `ownerEmail` | string   | Unique owner. Owner is not duplicated into `admins`.        |
| `admins`     | string[] | Admin emails. Admins can manage regular members and scopes. |
| `members`    | string[] | Regular member emails.                                      |
| `scopes`     | string[] | Shared remote scope keys in `email/alias/rootName` format.  |

Concurrency:

- `GroupServiceImpl` protects reads/writes with a `ReentrantLock`.
- Any new mutation should keep read-modify-write inside the same lock.

Integrity:

- Member/admin additions validate that the email exists in MySQL.
- Owner cannot be removed as owner through admin operations.
- Removing an admin demotes that user to a member when appropriate.
- Task deletion checks this file before removing remote storage.

---

## 5. Derived Response Models

### 5.1 `GroupInfo`

Returned by `/client/group/files` and `/server/group/files`.

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

Returned by `/server/file/list-scopes`.

```json
{
  "alias": "Work",
  "rootName": "Documents",
  "isDir": true,
  "scopeName": "alice@example.com/Work/Documents"
}
```

---

## 6. Migration Notes

There is an older server-side SQLite schema under:

```text
server/src/main/resources/backend/mapper/sqlite/scheme.sql
```

That file is not the authoritative schema for the current central server runtime. Current server identity persistence uses MySQL/MariaDB through `server/src/main/resources/db/mysql-init.sql`.

Docker profile can use `ScopeStorageMigrationRunner` to migrate older remote storage layouts toward the `email/alias/rootName` layout. The manual endpoint is:

```http
POST /server/file/migrate-legacy-storage
```

---

## 7. Files Ignored By Git

Runtime/generated storage is intentionally ignored:

- `groups.json`
- `*.db`, `*.sqlite`, `*.sqlite-wal`, `*.sqlite-shm`
- `log/`, `logs/`, `*.log`
- `server/target/`, `client-app/target/`
- `sync-app/out/`, `sync-app/dist/`, desktop installers/packages
- private keys and certificates such as `*.pem`, `*.key`, `*.p12`

Do not commit real credentials or local runtime databases.
