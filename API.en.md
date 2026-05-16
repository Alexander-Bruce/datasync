# DataSync API Reference

**English | [中文](API.md)**

This document describes the current REST API exposed by `server` and `client-app`.

---

## Conventions

| Service      | Base URL                                                                           | Description                                                                  |
| ------------ | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `server`     | `http://localhost:8090` locally, `https://<space>.hf.space` on Hugging Face Spaces | Central service for users, remote files, groups, avatars, and health checks. |
| `client-app` | `http://127.0.0.1:8092`                                                            | Local desktop agent used by the Electron renderer.                           |

Unless explicitly noted:

- Request bodies are JSON.
- `client-app` returns a unified `ResultEntity` envelope.
- Most `server` JSON endpoints also return `ResultEntity`.
- File byte endpoints can return raw `application/octet-stream`.
- The renderer sends `Authorization: Bearer <token>` to `client-app` after login.
- The current server security config permits `/server/**`, but validates Bearer tokens when present. Treat endpoint business checks as part of the security model.

Response envelope:

```json
{
  "code": 200,
  "message": "OK",
  "time": 1760000000,
  "data": {}
}
```

Frontend note: `sync-app/src/renderer/src/utils/request.js` unwraps only the HTTP response, not `data.data`. Callers usually receive the full envelope body and then read `res.data`.

---

## Common Models

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

## 1. Health

### 1.1 Server Health

```http
GET /
GET /health
```

Service: `server`

Response:

```json
{
  "status": "ok",
  "service": "datasync-server"
}
```

Used by the desktop setup page when testing the remote server URL.

---

## 2. Client Runtime Configuration

These endpoints are exposed by `client-app` and are used before login.

### 2.1 Get Client Configuration

```http
GET /client/config
```

Response `data`: `ClientConfig`.

Behavior:

- Reads `~/.datasync/client-config.json`.
- Returns `{ configured: false, syncPort: 8080 }` when missing or invalid.

### 2.2 Save Client Configuration

```http
POST /client/config
```

Request body:

| Field           | Type   | Required | Description                                                                     |
| --------------- | ------ | -------- | ------------------------------------------------------------------------------- |
| `serverBaseUrl` | string | yes      | Central server HTTP(S) base URL. Scheme is auto-filled as `http://` if omitted. |
| `syncHost`      | string | no       | Legacy Netty host. Defaults to host parsed from `serverBaseUrl`.                |
| `syncPort`      | number | no       | Legacy Netty port. Defaults to `8080`.                                          |

Response `data`: normalized `ClientConfig`.

Validation:

- `serverBaseUrl` must contain a valid host.
- `syncPort` must be between 1 and 65535.

### 2.3 Test Client Configuration

```http
POST /client/config/test
```

Request body: same as [2.2](#22-save-client-configuration).

Response `data`:

```json
{
  "serverBaseUrl": "https://example.com",
  "syncHost": "example.com",
  "syncPort": 8080,
  "httpStatus": 200,
  "reachable": true
}
```

Behavior:

- Normalizes the config.
- Sends `GET /` to the central server.
- Returns `code: 424` if unreachable.

---

## 3. Authentication

Authentication endpoints are exposed by `server` under `/unauthorized` and proxied by `client-app` under `/client/user`.

### 3.1 Server Login

```http
POST /unauthorized/login
```

Service: `server`

Request body:

| Field      | Type   | Required | Description                                 |
| ---------- | ------ | -------- | ------------------------------------------- |
| `email`    | string | yes      | User email.                                 |
| `password` | string | yes      | Plain text password submitted over HTTP(S). |

Response `data`:

```json
{
  "token": "jwt-token",
  "id": "1",
  "username": "Alice",
  "email": "alice@example.com",
  "avatar": ""
}
```

### 3.2 Server Signup

```http
POST /unauthorized/signup
```

Service: `server`

Request body:

| Field      | Type   | Required | Description                                 |
| ---------- | ------ | -------- | ------------------------------------------- |
| `username` | string | yes      | Display name.                               |
| `email`    | string | yes      | Email. Unique in MySQL.                     |
| `password` | string | yes      | Plain text password submitted over HTTP(S). |

Response `data`: same shape as login.

Behavior:

- Creates a new user with BCrypt-hashed password.
- If the email already exists and password matches, returns login data.
- If the email exists and password does not match, login fails.

### 3.3 Client Login

```http
POST /client/user/login
```

Service: `client-app`

Request body:

```json
{
  "email": "alice@example.com",
  "password": "secret"
}
```

Response `data`: local cached `User`.

Behavior:

- Calls server login.
- Upserts local SQLite `User` by email.
- Preserves server `id` in the local cache.

### 3.4 Client Signup

```http
POST /client/user/signup
```

Service: `client-app`

Request body:

```json
{
  "username": "Alice",
  "email": "alice@example.com",
  "password": "secret"
}
```

Response `data`: local cached `User`.

---

## 4. User APIs

### 4.1 Update Current User Through Client

```http
POST /client/user/update
```

Service: `client-app`

Request body: `User`.

Typical body:

```json
{
  "id": 1,
  "username": "Alice Chen",
  "email": "alice@example.com",
  "avatar": "data:image/png;base64,...",
  "refreshToken": "jwt-token"
}
```

Response `data`: updated local cached `User`.

Behavior:

- Forwards to `POST /server/user/update`.
- Mirrors returned central fields into SQLite.
- Avatar can be an existing URL or a supported `data:image/*;base64,...` value.

### 4.2 Verify Local Session

```http
POST /client/user/session
```

Service: `client-app`

Request body:

| Field   | Type   | Required | Description                       |
| ------- | ------ | -------- | --------------------------------- |
| `id`    | string | no       | Server user ID from localStorage. |
| `email` | string | yes      | Email from localStorage.          |

Response `data`: local cached `User`.

Returns `401` when the local SQLite user cache is missing or mismatched.

### 4.3 Restore Cached Session

```http
POST /client/user/session/current
```

Service: `client-app`

Request body: `{}`.

Response `data`: most recent cached local `User`.

Used by the router when Electron localStorage has no token but local SQLite still has a cached session.

### 4.4 Search Users Through Client

```http
POST /client/user/search
```

Service: `client-app`

Request body:

```json
{ "q": "alice" }
```

Response `data`: array of user summaries from the server.

### 4.5 Server User Search

```http
POST /server/user/search
```

Service: `server`

Request body:

```json
{ "q": "alice" }
```

Response `data`:

```json
[
  {
    "email": "alice@example.com",
    "username": "Alice",
    "avatar": "https://example.com/resources/avatars/1.png?v=1760000000"
  }
]
```

Behavior:

- Empty query returns an empty list.
- MySQL search is fuzzy over user fields implemented by `UserMapper.searchByQuery`.

### 4.6 Resolve User By Email

```http
POST /server/user/resolve
```

Service: `server`

Request body:

```json
{ "email": "alice@example.com" }
```

Response `data`:

```json
{
  "id": "1",
  "username": "Alice",
  "email": "alice@example.com",
  "avatar": ""
}
```

Used to rebuild local identity cache when a desktop client is reinstalled.

### 4.7 Server User Update

```http
POST /server/user/update
```

Service: `server`

Request body: server `User`.

Response `data`:

```json
{
  "id": "1",
  "username": "Alice Chen",
  "email": "alice@example.com",
  "avatar": "https://example.com/resources/avatars/1.png?v=1760000000"
}
```

Validation:

- `id` is required.
- If an authenticated principal exists and its id differs from the body id, the server returns `403`.
- Supported avatar data URL MIME types: `image/png`, `image/jpeg`, `image/webp`, `image/gif`.
- Avatar images must be 2 MB or smaller.

---

## 5. Client File Tree APIs

These endpoints are local `client-app` endpoints used by the desktop UI.

### 5.1 List Sync Tasks

```http
POST /client/file/brief-list
```

Request body:

```json
{ "email": "alice@example.com" }
```

Response `data`: `File[]` for the local user.

### 5.2 Delete Local File Tree Entry

```http
POST /client/file/delete
```

Request body:

```json
{
  "email": "alice@example.com",
  "path": "Documents/report.docx"
}
```

Response `data`: `true`.

This deletes local `SubFile` rows for the target path. It is not the same as deleting a sync task; use `/client/sync/delete` for task deletion.

### 5.3 List Root Children

```http
POST /client/file/detail-list
```

Request body:

```json
{ "fileId": "10" }
```

Response `data`: root-level `SubFile[]` where `parent` is null.

### 5.4 List Children Of A Subfolder

```http
POST /client/file/detail-list-parent
```

Request body:

```json
{
  "fileId": "101",
  "originalId": "10"
}
```

Response `data`: `SubFile[]` with `parent = fileId`.

Current implementation only reads `fileId`; `originalId` is retained for UI/path context compatibility.

### 5.5 List Remote Scopes For Restore

```http
POST /client/file/remote-scopes
```

Request body:

```json
{ "email": "alice@example.com" }
```

Response `data`: `RemoteScope[]`.

Behavior:

- Forwards to `POST /server/file/list-scopes`.
- Does not write local SQLite by itself.
- Used when the local desktop has no tasks but the remote server already has stored scopes.

---

## 6. Client Sync APIs

### 6.1 Upload Sync

```http
POST /client/sync/upload
```

Request body:

| Field    | Type   | Required                 | Description                                                       |
| -------- | ------ | ------------------------ | ----------------------------------------------------------------- |
| `fileId` | string | no                       | Preferred task id. `null`, empty, or absent falls back to `path`. |
| `email`  | string | yes                      | Current user email.                                               |
| `path`   | string | yes when `fileId` absent | Local task root path.                                             |

Response `data`: `true`.

Behavior:

1. Loads the local task for the user.
2. Scans files and builds CDC `SyncStyle` entries.
3. Builds scope key `email/alias/rootName`.
4. Calls `POST /server/file/compare`.
5. Uploads file bytes through `POST /server/file/upload`.
6. Marks local task and subfiles as synced.

### 6.2 Download Sync

```http
POST /client/sync/download
```

Request body: same as upload.

Response `data`: `true`.

Behavior:

1. Loads the local task.
2. Builds scope key `email/alias/rootName`.
3. Calls `POST /server/file/download`.
4. Downloads every relative path through `POST /server/file/download/file`.
5. Writes bytes to the local target and overwrites existing files.
6. Rebuilds local `SubFile` rows and marks the task as synced.

### 6.3 Create Or Update Sync Task

```http
POST /client/sync/update
```

Request body:

| Field         | Type           | Required | Description                                                            |
| ------------- | -------------- | -------- | ---------------------------------------------------------------------- |
| `fileId`      | string         | no       | Existing `File.id`; absent/null/empty creates a new task.              |
| `email`       | string         | yes      | Current user email.                                                    |
| `alias`       | string         | yes      | Unique task alias for the user.                                        |
| `path`        | string         | yes      | Absolute local path to file or folder.                                 |
| `remoteHost`  | string         | no       | Metadata/legacy host field. Runtime HTTP URL comes from client config. |
| `scheduled`   | string         | no       | Interval string such as `5m`, `1h`, `1d`, `never`.                     |
| `cdcAlg`      | string         | yes      | `FastCDC`, `FlipCDC`, `QuickCDC`, or `RabinCDC`.                       |
| `description` | string         | no       | Task description.                                                      |
| `isDir`       | string/boolean | yes      | Whether `path` is a directory.                                         |

Response `data`: saved `File`.

Validation:

- Alias cannot be blank.
- Path cannot be blank.
- Alias must be unique for the local user.
- Existing task must belong to the local user.

### 6.4 Delete Sync Task

```http
POST /client/sync/delete
```

Request body:

| Field    | Type   | Required                 | Description         |
| -------- | ------ | ------------------------ | ------------------- |
| `fileId` | string | no                       | Preferred task id.  |
| `email`  | string | yes                      | Current user email. |
| `path`   | string | yes when `fileId` absent | Local task path.    |

Response `data`: `true`.

Behavior:

1. Loads local task.
2. Builds scope key.
3. Calls `POST /server/group/check-scope`.
4. If any group references the scope, returns `409`.
5. Calls `POST /server/file/delete-scope`.
6. Deletes local `SubFile` and `File` rows.

---

## 7. Server File APIs

These endpoints are called by `client-app`.

### 7.1 Compare Remote Scope

```http
POST /server/file/compare
```

Request body:

| Field       | Type          | Required | Description                                    |
| ----------- | ------------- | -------- | ---------------------------------------------- |
| `email`     | string        | yes      | Owner email.                                   |
| `path`      | string        | yes      | Client local root path.                        |
| `scopeName` | string        | yes      | Full remote scope key: `email/alias/rootName`. |
| `isDir`     | boolean       | yes      | Whether the task root is a directory.          |
| `list`      | `SyncStyle[]` | yes      | Client file list with `storagePath` values.    |

Response `data`: `SyncStyle[]`.

Current behavior returns the input list after cleaning stale remote files. The client uploads each item in the list over HTTP.

### 7.2 List Download Files

```http
POST /server/file/download
```

Request body:

```json
{ "scopeName": "alice@example.com/Work/Documents" }
```

Response `data`:

```json
["report.docx", "subdir/notes.txt"]
```

Behavior:

- Returns relative file paths.
- Skips `.part` temporary files.
- For single-file scopes, returns the file name.

### 7.3 Download One File

```http
POST /server/file/download/file
Content-Type: application/json
Accept: application/octet-stream
```

Request body:

```json
{
  "scopeName": "alice@example.com/Work/Documents",
  "relativePath": "subdir/notes.txt"
}
```

Response: raw file bytes with `Content-Type: application/octet-stream`.

Path traversal outside `scopeName` is rejected.

### 7.4 Upload One File

```http
POST /server/file/upload?storagePath=<path>&fileName=<name>
Content-Type: application/octet-stream
```

Query parameters:

| Field         | Type   | Required | Description                                                                                     |
| ------------- | ------ | -------- | ----------------------------------------------------------------------------------------------- |
| `storagePath` | string | yes      | Remote directory relative to `basePath`, for example `alice@example.com/Work/Documents/subdir`. |
| `fileName`    | string | yes      | File name only. Path separators, `.`, and `..` are rejected.                                    |

Request body: raw file bytes.

Response `data`:

```json
{ "bytes": 12345 }
```

Behavior:

- Writes to `<fileName>.part`.
- Replaces the final file after the copy completes.
- Keeps target path inside the configured `basePath`.

### 7.5 Delete Remote Scope

```http
POST /server/file/delete-scope
```

Request body:

```json
{ "scopeName": "alice@example.com/Work/Documents" }
```

Response `data`: `true`.

Deletes the scope file/folder and cleans empty parent directories until `basePath`.

### 7.6 List User Remote Scopes

```http
POST /server/file/list-scopes
```

Request body:

```json
{ "email": "alice@example.com" }
```

Response `data`: `RemoteScope[]`.

Scans `basePath/<email>/` and returns discoverable task scopes.

### 7.7 Migrate Legacy Storage

```http
POST /server/file/migrate-legacy-storage
```

Request body: `{}`.

Response `data`:

```json
{
  "scanned": 10,
  "migrated": 8,
  "skipped": 2,
  "failed": 0,
  "groupScopes": 3
}
```

Only available when `ScopeStorageMigrationRunner` is active. Non-docker profiles return `409`.

---

## 8. Client Group APIs

All endpoints below are exposed by `client-app` under `/client/group` and forwarded to the central server, except `download-scope`, which starts a local background download.

### 8.1 Create Group

```http
POST /client/group/create
```

Request:

```json
{ "email": "alice@example.com", "name": "Team Docs" }
```

Response `data`: `Group`.

### 8.2 Add Member

```http
POST /client/group/add-member
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmail": "carol@example.com"
}
```

Response `data`: updated `Group`.

### 8.3 Remove Member

```http
POST /client/group/remove-member
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmail": "carol@example.com"
}
```

Response `data`: updated `Group`.

### 8.4 Add Shared Scope

```http
POST /client/group/add-scope
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "scopeName": "alice@example.com/Work/Documents"
}
```

Response `data`: updated `Group`.

### 8.5 Remove Shared Scope

```http
POST /client/group/remove-scope
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "scopeName": "alice@example.com/Work/Documents"
}
```

Response `data`: updated `Group`.

### 8.6 Delete Group

```http
POST /client/group/delete
```

Request:

```json
{ "email": "alice@example.com", "groupId": "uuid" }
```

Response `data`: `true`.

Owner-only.

### 8.7 List Groups

```http
POST /client/group/list
```

Request:

```json
{ "email": "alice@example.com" }
```

Response `data`: groups where the user is owner, admin, or member.

### 8.8 List Group Files

```http
POST /client/group/files
```

Request:

```json
{ "email": "alice@example.com" }
```

Response `data`:

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

### 8.9 Add Admin

```http
POST /client/group/add-admin
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "adminEmail": "bob@example.com"
}
```

Response `data`: updated `Group`.

Owner-only.

### 8.10 Remove Admin

```http
POST /client/group/remove-admin
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "adminEmail": "bob@example.com"
}
```

Response `data`: updated `Group`.

Owner-only. Removing an admin demotes the user to a regular member when appropriate.

### 8.11 Add Members In Batch

```http
POST /client/group/add-members
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmails": ["carol@example.com", "dave@example.com"]
}
```

Response `data`: updated `Group`.

### 8.12 Remove Members In Batch

```http
POST /client/group/remove-members
```

Request:

```json
{
  "email": "alice@example.com",
  "groupId": "uuid",
  "memberEmails": ["carol@example.com", "dave@example.com"]
}
```

Response `data`: updated `Group`.

### 8.13 Download Group Scope

```http
POST /client/group/download-scope
```

Request:

| Field          | Type   | Required | Description                                                                  |
| -------------- | ------ | -------- | ---------------------------------------------------------------------------- |
| `email`        | string | no       | Current user email; currently not used by implementation.                    |
| `scopeName`    | string | yes      | Full remote scope key.                                                       |
| `localPath`    | string | yes      | Local target directory.                                                      |
| `relativePath` | string | no       | Optional file/folder path inside the scope. Empty downloads the whole scope. |

Response `data`: `null`.

Behavior:

- Returns immediately.
- Starts a background task that downloads files from the scope into `localPath`.
- If `relativePath` points to a file, downloads only that file.
- If `relativePath` points to a folder prefix, downloads matching files under that prefix.

---

## 9. Server Group APIs

Server group endpoints mirror the client group forwarding endpoints:

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

### 9.1 Role Authorization

| Operation                 | Required role                                 |
| ------------------------- | --------------------------------------------- |
| Create group              | Any caller email can create its own group.    |
| Delete group              | Owner                                         |
| Add/remove admin          | Owner                                         |
| Add/remove regular member | Owner or admin                                |
| Add/remove scope          | Owner or admin                                |
| List groups/files         | Owner, admin, or member can see their groups. |
| Check scope               | No role check; used by client deletion guard. |

### 9.2 Check Whether A Scope Can Be Deleted

```http
POST /server/group/check-scope
```

Request:

```json
{
  "email": "alice@example.com",
  "scopeName": "alice@example.com/Work/Documents"
}
```

Response `data`: `true` if the scope is not referenced by any group; otherwise `false`.

---

## 10. Client Logs

### 10.1 Tail Local Client Log

```http
GET /client/log/list?lines=300
```

Service: `client-app`

Query parameters:

| Field   | Type   | Default | Description                         |
| ------- | ------ | ------- | ----------------------------------- |
| `lines` | number | `300`   | Number of trailing lines to return. |

Response `data`: `string[]`.

Reads:

```text
log/log.log
```

relative to the `client-app` working directory.

---

## 11. Error Handling

Most errors are returned with HTTP 200 and a non-2xx `code` in the response envelope:

```json
{
  "code": 409,
  "message": "Task name already exists",
  "time": 1760000000,
  "data": null
}
```

Some raw-byte or low-level HTTP failures can use non-2xx HTTP status codes. `HttpJsonClient` treats either a non-2xx HTTP status or a non-2xx envelope `code` as failure.

Common codes:

|  Code | Meaning                                                                |
| ----: | ---------------------------------------------------------------------- |
| `400` | Invalid or missing request data.                                       |
| `401` | Login/session/token failure.                                           |
| `403` | Role or profile ownership denied.                                      |
| `404` | Target user/group/resource not found.                                  |
| `409` | Business conflict, such as duplicate task alias or protected deletion. |
| `424` | Remote server connection test failed.                                  |
| `500` | Unexpected server/client backend failure.                              |
