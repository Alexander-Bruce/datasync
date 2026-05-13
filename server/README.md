---
title: DataSync Server
sdk: docker
app_port: 7860
---

# DataSync Server

Docker Space deployment for the DataSync Spring Boot server.

The Docker profile reads runtime configuration from environment variables. If database secrets are
missing, login and signup requests fail with `CannotGetJdbcConnectionException`.

Set the following Hugging Face Space secrets before rebuilding:

- `MYSQL_PASSWORD`
- `REDIS_MASTER_PASSWORD`
- `REDIS_SLAVE_PASSWORD`
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
