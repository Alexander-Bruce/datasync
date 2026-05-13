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
- Netty sync: `8080`
- Sync storage path: `/sync`
- MariaDB data path: `/tmp/mysql`

Redis is not packaged because the current server code path does not use it. If `MYSQL_URL` is
overridden to an external database, remember that Hugging Face Spaces only allow outbound requests
to ports `80`, `443`, and `8080`.
