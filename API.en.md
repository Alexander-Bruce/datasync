# DataSync API Documentation

**[English](API.en.md) | [中文](API.md)**

> Conventions:
> - **Server** (Spring Boot) runs at `http://localhost:8090`
> - **Client** (Spring Boot) runs at `http://localhost:8092`
> - All request / response bodies are JSON
> - All authenticated endpoints require `Authorization: Bearer <access_token>` in the header
> - Unified response envelope:
>
> ```json
> {
>   "code": 200,
>   "message": "...",
>   "data": { ... }
> }
> ```

---

## 1. Authentication (Server, `/unauthorized`, no auth required)

### 1.1 Login

```
POST /unauthorized/login
```

**Request body**

| Field    | Type   | Description       |
| -------- | ------ | ----------------- |
| email    | string | Email address     |
| password | string | Plain-text password |

**Response `data`**

| Field         | Type   | Description           |
| ------------- | ------ | --------------------- |
| access_token  | string | JWT Access Token      |
| refresh_token | string | JWT Refresh Token     |

---

### 1.2 Register

```
POST /unauthorized/signup
```

**Request body**

| Field    | Type   | Description       |
| -------- | ------ | ----------------- |
| username | string | Display name      |
| email    | string | Email address     |
| password | string | Plain-text password |

**Response `data`**: The newly created user object

---

### 1.3 Refresh Access Token

```
GET /unauthorized/refesh-token
```

**Query parameters**

| Parameter | Type   | Description |
| --------- | ------ | ----------- |
| id        | string | User ID     |

**Response `data`**: New `access_token`

---

## 2. Client User Endpoints (Client, `/client/user`)

### 2.1 Login (client-local)

```
POST /client/user/login
```

**Request body**

| Field    | Type   | Description         |
| -------- | ------ | ------------------- |
| email    | string | Email address       |
| password | string | Plain-text password |

---

### 2.2 Register (client-local)

```
POST /client/user/signup
```

**Request body**

| Field    | Type   | Description         |
| -------- | ------ | ------------------- |
| username | string | Display name        |
| email    | string | Email address       |
| password | string | Plain-text password |

---

## 3. File List Endpoints (Client, `/client/file`)

### 3.1 Get Sync Task List (Brief List)

```
POST /client/file/brief-list
```

**Request body**

| Field | Type   | Description          |
| ----- | ------ | -------------------- |
| email | string | Current user's email |

**Response `data`**: `File[]` — see `Client.File` in [Database Tables.en.md](./Database%20Tables.en.md)

---

### 3.2 Get Root-Level Children of a Directory (Detail List)

```
POST /client/file/detail-list
```

**Request body**

| Field  | Type   | Description        |
| ------ | ------ | ------------------ |
| fileId | string | Primary key of the File record |

**Response `data`**: `SubFile[]` — root-level items where `parent` is null

---

### 3.3 Get Children of a Sub-Directory

```
POST /client/file/detail-list-parent
```

**Request body**

| Field      | Type   | Description                                 |
| ---------- | ------ | ------------------------------------------- |
| fileId     | string | SubFile id of the target folder             |
| originalId | string | Root File id (used for path validation)     |

**Response `data`**: `SubFile[]` — direct children of the specified folder

---

## 4. Sync Operation Endpoints (Client, `/client/sync`)

### 4.1 Upload Sync

Syncs local files to the server and updates local `is_sync = true` on completion.

```
POST /client/sync/upload
```

**Request body**

| Field | Type   | Description                        |
| ----- | ------ | ---------------------------------- |
| email | string | Current user's email               |
| path  | string | Absolute local root path of the sync task |

**Response `data`**: `true` / `false`

**Flow**
1. Look up the local SQLite `File` record by `path` and `email` to get the CDC algorithm class name
2. Chunk local files with the CDC algorithm; each `SyncStyle.storagePath` is built as `email/folderName/...` (e.g. `alice@example.com/Documents/subdir`) to avoid collisions between users with identically named folders
3. Call `POST /server/file/compare` (with `email`); server writes files under `basePath/email/folderName/`
4. Send delta chunks (encrypted) to the server via Netty; server writes them to the corresponding sub-directory
5. Update local `File.is_sync` and all `SubFile.is_sync` to `true`

---

### 4.2 Download Sync

Pulls server-stored files to local storage and overwrites existing content.

```
POST /client/sync/download
```

**Request body**

| Field | Type   | Description                        |
| ----- | ------ | ---------------------------------- |
| email | string | Current user's email               |
| path  | string | Absolute local root path of the sync task |

**Response `data`**: `true` / `false`

**Flow**
1. Look up the local `File` record by `path`; `scopeName = email + "/" + last path segment` (e.g. `alice@example.com/Documents`)
2. Call `POST /server/file/download` (passing `scopeName`); server scans `basePath/email/folderName/`
3. Write each file to `path/relativePath` locally, overwriting existing files
4. Update local `File.is_sync` and `SubFile.is_sync` to `true`

---

### 4.3 Create / Update Sync Task

```
POST /client/sync/update
```

**Request body**

| Field       | Type   | Description                         |
| ----------- | ------ | ----------------------------------- |
| fileId      | string | File record id (null for new tasks) |
| email       | string | Current user's email                |
| alias       | string | Task alias                          |
| path        | string | Absolute local path                 |
| remoteHost  | string | Remote host address                 |
| scheduled   | string | Cron expression (optional)          |
| cdcAlg      | string | CDC algorithm name                  |
| description | string | Task description (optional)         |

**Response `data`**: Updated `File` object

---

### 4.4 Delete Sync Task

```
POST /client/sync/delete
```

**Request body**

| Field | Type   | Description               |
| ----- | ------ | ------------------------- |
| email | string | Current user's email      |
| path  | string | Path of the task to delete |

**Response `data`**: `true` / `false`

**Pre-check**: Before deleting, the client calls `POST /server/group/check-scope` to verify whether the task's scope (`email/folderName`) is still referenced by a group with active members. If so, `code: 409` is returned with an error message; deletion is aborted and the frontend displays the error in the confirmation dialog. The user must remove the related group first.

---

## 5. Server File Operation Endpoints (Server, `/server/file`)

> These endpoints are called internally by the client; they are not exposed directly to the frontend.

### 5.1 Delta Compare

```
POST /server/file/compare
```

**Request body**

| Field | Type        | Description                                                              |
| ----- | ----------- | ------------------------------------------------------------------------ |
| email | string      | Current user's email (used to build `email/folderName` storage path)    |
| path  | string      | Client's local root path (used to derive the last segment of the server scope directory) |
| list  | SyncStyle[] | Client's file chunking info list (`storagePath` already includes email prefix) |

**Server scope directory**: `basePath/email/folderName/` (e.g. `C:\Sync\alice@example.com\Documents\`), ensuring isolation for users with same-named folders.

**Response `data`**: `SyncStyle[]` — files that need to be transferred (missing or changed on server)

**SyncStyle fields**

| Field       | Type   | Description                                                                   |
| ----------- | ------ | ----------------------------------------------------------------------------- |
| file        | File   | File object (includes absolute path)                                          |
| syncType    | enum   | FastCDC / FlipCDC / RabinCDC / QuickCDC                                       |
| storagePath | string | Server-side relative storage path (format: `email/folderName` or `email/folderName/subdir`) |

---

### 5.2 Download File List

```
POST /server/file/download
```

**Request body**

| Field     | Type   | Description                                          |
| --------- | ------ | ---------------------------------------------------- |
| scopeName | string | Sync scope path in the format `email/folderName` (e.g. `alice@example.com/Documents`) |

**Response `data`**: Relative paths of all files under the server's scope directory — `string[]` (content not included).

File content is fetched individually via `POST /server/file/download/file`.

---

## 6. Group Management Endpoints (Client, `/client/group`)

> These endpoints are called by the frontend; the client does not store group data — it forwards requests directly to the server.

### 6.1 Create Group

```
POST /client/group/create
```

**Request body**

| Field | Type   | Description          |
| ----- | ------ | -------------------- |
| email | string | Current user's email |
| name  | string | Group name           |

**Response `data`**: Newly created `Group` object

---

### 6.2 Add Member

```
POST /client/group/add-member
```

**Request body**

| Field       | Type   | Description              |
| ----------- | ------ | ------------------------ |
| email       | string | Current user's email     |
| groupId     | string | Group id                 |
| memberEmail | string | Email of the member to add |

**Response `data`**: Updated `Group` object

---

### 6.3 Remove Member

```
POST /client/group/remove-member
```

**Request body**

| Field       | Type   | Description                 |
| ----------- | ------ | --------------------------- |
| email       | string | Current user's email        |
| groupId     | string | Group id                    |
| memberEmail | string | Email of the member to remove |

**Response `data`**: Updated `Group` object

---

### 6.4 Add Shared Folder

```
POST /client/group/add-scope
```

**Request body**

| Field     | Type   | Description                                                                                          |
| --------- | ------ | ---------------------------------------------------------------------------------------------------- |
| email     | string | Current user's email                                                                                 |
| groupId   | string | Group id                                                                                             |
| scopeName | string | Sync scope key in the format `ownerEmail/folderName` (e.g. `alice@example.com/Work`); auto-built by the frontend from the current user email + selected task path's last segment |

**Response `data`**: Updated `Group` object

---

### 6.5 Remove Shared Folder

```
POST /client/group/remove-scope
```

**Request body**

| Field     | Type   | Description          |
| --------- | ------ | -------------------- |
| email     | string | Current user's email |
| groupId   | string | Group id             |
| scopeName | string | Sync scope name      |

**Response `data`**: Updated `Group` object

---

### 6.6 Delete Group

```
POST /client/group/delete
```

**Request body**

| Field   | Type   | Description          |
| ------- | ------ | -------------------- |
| email   | string | Current user's email |
| groupId | string | Group id             |

**Response `data`**: `true`

---

### 6.7 Get Group List

```
POST /client/group/list
```

**Request body**

| Field | Type   | Description          |
| ----- | ------ | -------------------- |
| email | string | Current user's email |

**Response `data`**: `Group[]` — all groups the current user owns or belongs to

**Group fields**

| Field      | Type     | Description                                                                     |
| ---------- | -------- | ------------------------------------------------------------------------------- |
| id         | string   | Group UUID                                                                      |
| name       | string   | Group name                                                                      |
| ownerEmail | string   | Owner's email                                                                   |
| members    | string[] | Member email list                                                               |
| scopes     | string[] | Shared folder key list; each item is in the format `ownerEmail/folderName`     |

---

### 6.8 Get Group File Structure

```
POST /client/group/files
```

**Request body**

| Field | Type   | Description          |
| ----- | ------ | -------------------- |
| email | string | Current user's email |

**Response `data`**: `GroupInfo[]` — file trees for all shared scopes in each group

**GroupInfo fields**

| Field      | Type             | Description                                   |
| ---------- | ---------------- | --------------------------------------------- |
| id         | string           | Group UUID                                    |
| name       | string           | Group name                                    |
| ownerEmail | string           | Owner's email                                 |
| owner      | boolean          | Whether the current user is the group owner   |
| members    | string[]         | Member email list                             |
| scopes     | GroupScopeInfo[] | Shared folders and their file lists           |

**GroupScopeInfo fields**

| Field     | Type            | Description                                                                                       |
| --------- | --------------- | ------------------------------------------------------------------------------------------------- |
| scopeName | string          | Folder key (`ownerEmail/folderName`); the UI displays only the part after `/`                    |
| files     | GroupFileNode[] | Flat list of all files in the scope (all depth levels)                                           |

**GroupFileNode fields**

| Field        | Type    | Description                               |
| ------------ | ------- | ----------------------------------------- |
| name         | string  | File / folder name                        |
| relativePath | string  | Path relative to the scope root directory |
| dir          | boolean | Whether it is a directory                 |

---

### 6.9 Async Download Sync (Group Scope)

```
POST /client/group/download-scope
```

Returns immediately; downloads all files under the server's scope directory to the specified local path in the background.

**Request body**

| Field     | Type   | Description                       |
| --------- | ------ | --------------------------------- |
| email     | string | Current user's email              |
| scopeName | string | Sync scope name to download       |
| localPath | string | Local root directory for saving   |

**Response `data`**: `null` (task has been started in the background)

---

## 7. Group Management Endpoints (Server, `/server/group`)

> These endpoints are called internally by the client; they are not exposed directly to the frontend. Group data is stored in `basePath/groups.json`.

### 7.1 Create Group

```
POST /server/group/create
```

**Request body**: `{ email, name }`  
**Response `data`**: Newly created `Group` object

---

### 7.2 Add Member

```
POST /server/group/add-member
```

**Request body**: `{ email, groupId, memberEmail }`  
**Response `data`**: Updated `Group` object

---

### 7.3 Remove Member

```
POST /server/group/remove-member
```

**Request body**: `{ email, groupId, memberEmail }`  
**Response `data`**: Updated `Group` object

---

### 7.4 Add Shared Folder

```
POST /server/group/add-scope
```

**Request body**: `{ email, groupId, scopeName }`  
**Response `data`**: Updated `Group` object

---

### 7.5 Remove Shared Folder

```
POST /server/group/remove-scope
```

**Request body**: `{ email, groupId, scopeName }`  
**Response `data`**: Updated `Group` object

---

### 7.6 Delete Group

```
POST /server/group/delete
```

**Request body**: `{ email, groupId }`  
**Response `data`**: `true`

---

### 7.7 Get Group List

```
POST /server/group/list
```

**Request body**: `{ email }`  
**Response `data`**: `Group[]`

---

### 7.8 Get Group File Structure

```
POST /server/group/files
```

**Request body**: `{ email }`  
**Response `data`**: `GroupInfo[]` — file tree built by traversing `basePath/email/folderName/`

---

### 7.9 Check Whether a Scope Can Be Deleted

```
POST /server/group/check-scope
```

**Request body**

| Field     | Type   | Description                                                            |
| --------- | ------ | ---------------------------------------------------------------------- |
| email     | string | Current user's email (used for logging only; validation is by scopeName) |
| scopeName | string | Scope key to check (`email/folderName`)                                |

**Response `data`**: `true` (deletable) / `false` (still referenced by a group with active members)

**Notes**: The client calls this before executing `POST /client/sync/delete`. If it returns `false`, the client throws `BaseException(409)` with an error message; the frontend shows the error in the delete confirmation dialog. The user must remove the related group first.

---

## 8. Netty Sync Protocol (Upload, Internal)

> The listening port is configured by `spring.netty.server.port` (default 8888).

**Handshake packet `StartSyncRequest`**

| Field           | Type   | Description                                          |
| --------------- | ------ | ---------------------------------------------------- |
| fileName        | string | File name                                            |
| storagePath     | string | Server-side relative storage path                   |
| cdcClassName    | string | Fully qualified CDC implementation class name        |
| EncryptedAESKey | byte[] | AES-256 key encrypted with RSA-OAEP-SHA256           |

**Data packet `SyncPacket`**

| Field | Type        | Description                                                        |
| ----- | ----------- | ------------------------------------------------------------------ |
| type  | enum        | `REFERENCE` (reuse existing chunk) / `DATA` (new chunk) / `EOF`   |
| hash  | string      | SHA-256 hash of the chunk (used for `REFERENCE` type)              |
| data  | byte[]      | AES-GCM encrypted chunk data (used for `DATA` type)               |

**Encryption**

- Data encryption: AES-256-GCM, random 12-byte IV, each `SyncPacket` encrypted independently
- Key exchange: RSA-2048 OAEP-SHA256; public key stored in `conf/rsa-public.pem`
