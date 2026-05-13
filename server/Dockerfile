FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=7860
ENV NETTY_SERVER_PORT=8080
ENV NETTY_CLIENT_PORT=8080
ENV NETTY_BASE_PATH=/sync
ENV MYSQL_DATABASE=datasync
ENV MYSQL_URL=jdbc:mysql://127.0.0.1:3306/datasync
ENV MYSQL_USERNAME=root
ENV MYSQL_PASSWORD=datasync
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      mariadb-client \
      mariadb-server \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/db /sync /data/mysql /run/mysqld \
    && chown -R mysql:mysql /data/mysql /run/mysqld

COPY --from=build /workspace/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
COPY src/main/resources/db/mysql-init.sql /app/mysql-init.sql
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 7860 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
