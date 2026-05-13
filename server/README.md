---
title: DataSync Server
sdk: docker
app_port: 7860
---

# DataSync Server

Docker Space deployment for the DataSync Spring Boot server.

The Docker image runs the Spring Boot server and an embedded MySQL-compatible MariaDB process in
the same container. The Docker profile defaults to the local database at
`jdbc:mysql://127.0.0.1:3306/datasync`.

The packaged desktop client syncs file content through the server HTTP API. For Hugging Face
Spaces, users should configure the client with:

- Server API URL: `https://<space-name>.hf.space`
- Sync host / port: kept only for legacy Netty settings; HTTP upload does not use them on Spaces

Set the following Hugging Face Space secrets before rebuilding:

- `MYSQL_PASSWORD`
- `JWT_SECRETKEY`
- `AWS_S3_ACCESSKEY`
- `AWS_S3_SECRETKEY`

Optional variables:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `NETTY_SERVER_PORT`
- `NETTY_CLIENT_PORT`
- `NETTY_BASE_PATH`
- `AWS_S3_ENDPOINT`
- `AWS_S3_BUCKET`

Default ports:

- HTTP: `7860`
- HTTP file upload/download sync: proxied through `7860` / HTTPS
- Legacy Netty sync: `8080`
- Sync storage path: `/sync`
- MariaDB data path: `/tmp/mysql`

Redis is not packaged because the current server code path does not use it. If `MYSQL_URL` is
overridden to an external database, remember that Hugging Face Spaces only allow outbound requests
to ports `80`, `443`, and `8080`.

When Hugging Face persistent storage is enabled, the container entrypoint links `/sync` to
`/data/sync`, so uploaded files survive Space restarts while the app can keep using `/sync`.
Without persistent storage, `/sync` is ephemeral and uploaded files may disappear after a rebuild or
restart.
