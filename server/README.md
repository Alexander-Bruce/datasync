---
title: DataSync Server
sdk: docker
app_port: 7860
---

# DataSync Server Deployment

This directory contains the central DataSync Spring Boot server and can be deployed as a Docker SDK Hugging Face Space.

---

## What Runs In This Image

The Docker image runs:

- Spring Boot `server` on port `7860`.
- Embedded MariaDB in the same container.
- The DataSync filesystem storage root at `/sync`.
- MySQL schema initialization from `src/main/resources/db/mysql-init.sql`.
- Optional persistent storage wiring from `/sync` to `/data/sync`.

The packaged desktop client uploads and downloads file content through the server HTTP API. Hugging Face Spaces do not expose arbitrary raw TCP ports for public client traffic, so the current desktop release should be configured with the Space HTTPS URL.

---

## Expected Client Configuration

For a Space named `my-datasync`:

```json
{
  "serverBaseUrl": "https://my-datasync.hf.space",
  "syncHost": "my-datasync.hf.space",
  "syncPort": 8080,
  "configured": true
}
```

Important:

- `serverBaseUrl` is used for current HTTP(S) upload/download.
- `syncHost` and `syncPort` are retained for legacy Netty settings.
- Do not use the container's internal MariaDB or Netty address in the desktop setup page.

---

## Required Secrets

Set these Hugging Face Space secrets before rebuilding:

| Secret             | Purpose                                                           |
| ------------------ | ----------------------------------------------------------------- |
| `MYSQL_PASSWORD`   | Password for the embedded MariaDB root user.                      |
| `JWT_SECRETKEY`    | JWT signing key. Generate with `openssl rand -base64 32`.         |
| `AWS_S3_ACCESSKEY` | Optional S3-compatible setting placeholder. Keep empty if unused. |
| `AWS_S3_SECRETKEY` | Optional S3-compatible setting placeholder. Keep empty if unused. |

The current default filesystem storage path does not require S3. S3 fields remain in config for compatibility/future storage work.

---

## Optional Environment Variables

| Variable                   | Default                                | Description                                                |
| -------------------------- | -------------------------------------- | ---------------------------------------------------------- |
| `SERVER_PORT`              | `7860`                                 | Spring Boot HTTP port in Docker.                           |
| `MYSQL_DATABASE`           | `datasync`                             | Database name created by the entrypoint.                   |
| `MYSQL_DATA_DIR`           | `/tmp/mysql`                           | MariaDB data directory inside the container.               |
| `MYSQL_URL`                | `jdbc:mysql://127.0.0.1:3306/datasync` | JDBC URL used by Spring Boot.                              |
| `MYSQL_USERNAME`           | `root`                                 | Database username.                                         |
| `NETTY_BASE_PATH`          | `/sync`                                | Server storage root for synced files, groups, and avatars. |
| `NETTY_SERVER_PORT`        | `8080`                                 | Legacy Netty server port.                                  |
| `NETTY_CLIENT_PORT`        | `8080`                                 | Legacy Netty client port setting.                          |
| `NETTY_CLIENT_HOST`        | `localhost`                            | Legacy Netty client host setting.                          |
| `PUBLIC_BASE_URL`          | empty                                  | Public base URL used when generating avatar URLs.          |
| `AWS_S3_ENDPOINT`          | `http://localhost`                     | Optional S3-compatible endpoint.                           |
| `AWS_S3_BUCKET`            | `datasync`                             | Optional S3-compatible bucket.                             |
| `AWS_S3_REGION`            | `auto`                                 | Optional S3-compatible region.                             |
| `AWS_S3_PATH_STYLE_ACCESS` | `true`                                 | Optional S3 path-style setting.                            |

---

## Ports

|   Port | Purpose                                                              |
| -----: | -------------------------------------------------------------------- |
| `7860` | Public Spring Boot HTTP API in Hugging Face Spaces.                  |
| `8080` | Legacy Netty raw TCP listener, useful only where raw TCP is exposed. |
| `3306` | MariaDB inside the container, bound to `127.0.0.1`.                  |

Only HTTP(S) traffic to the app port is expected for normal Space users.

---

## Persistent Storage

The server stores files under `NETTY_BASE_PATH`, defaulting to `/sync`.

When Hugging Face persistent storage is enabled, the entrypoint does this:

1. Checks whether `/data` exists and is writable.
2. Creates `/data/sync`.
3. Replaces empty `/sync` with a symlink to `/data/sync`.

Result:

```text
/sync -> /data/sync
```

This keeps uploaded files, `groups.json`, and avatars across Space restarts.

Without persistent storage, `/sync` is ephemeral. Uploaded files can disappear after rebuilds or restarts.

---

## Storage Layout

Inside `/sync`:

```text
/sync/
|-- <email>/
|   `-- <taskAlias>/
|       `-- <rootName>/
|           `-- <relative files>
|-- avatars/
|   `-- <userId>.<ext>
`-- groups.json
```

The canonical scope key is:

```text
<email>/<taskAlias>/<rootName>
```

Do not manually move files into an older `email/rootName` layout. Group sharing and deletion guards depend on the full scope key.

---

## Local Docker Build

From the repository root:

```bash
docker build -t datasync-server ./server
```

Run:

```bash
docker run --rm -p 7860:7860 \
  -e MYSQL_PASSWORD=change-me \
  -e JWT_SECRETKEY="$(openssl rand -base64 32)" \
  -v datasync-sync:/sync \
  datasync-server
```

Health check:

```bash
curl http://localhost:7860/health
```

---

## Running Without Docker

Prerequisites:

- Java 21.
- MySQL or MariaDB.
- A database named `datasync`.

Initialize:

```bash
mysql -u root -p datasync < src/main/resources/db/mysql-init.sql
```

Edit:

```text
src/main/resources/application-dev.yml
```

Start:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Local HTTP default:

```text
http://localhost:8090
```

---

## Avatar URLs

User avatar upload can save image files under:

```text
<basePath>/avatars/
```

The server exposes them through:

```text
/resources/avatars/<file>
```

If the server sits behind a reverse proxy or Space URL and generated avatar URLs are wrong, set:

```text
PUBLIC_BASE_URL=https://your-public-server-url
```

---

## Legacy Storage Migration

Docker profile can activate `ScopeStorageMigrationRunner`.

Manual endpoint:

```http
POST /server/file/migrate-legacy-storage
```

Response:

```json
{
  "scanned": 10,
  "migrated": 8,
  "skipped": 2,
  "failed": 0,
  "groupScopes": 3
}
```

Use this only when older remote storage layout entries need to be migrated into the `email/alias/rootName` layout.

---

## Operational Checklist

Before exposing the server:

- Set a strong `JWT_SECRETKEY`.
- Set a non-default `MYSQL_PASSWORD`.
- Enable persistent storage if uploaded files must survive restarts.
- Confirm `/health` returns `{"status":"ok","service":"datasync-server"}`.
- Register a test user from the desktop client.
- Upload a test folder and confirm files appear under `/sync/<email>/<alias>/<rootName>`.
- Restart the container and confirm uploaded files remain.
- Avoid committing local changes to `application-dev.yml` that contain real credentials.

---

## Known Limitations

- `groups.json` is file-based. For large multi-user deployments, move group metadata into a database.
- The current public desktop sync path uses HTTP(S), not raw Netty.
- The Docker image embeds MariaDB for convenience. Production deployments may prefer an external managed database.
- If Hugging Face persistent storage is not enabled, synced files are not durable.
