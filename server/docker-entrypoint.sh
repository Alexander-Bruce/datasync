#!/usr/bin/env bash
set -euo pipefail

MYSQL_DATA_DIR="${MYSQL_DATA_DIR:-/tmp/mysql}"
MYSQL_SOCKET="${MYSQL_SOCKET:-/run/mysqld/mysqld.sock}"
MYSQL_DATABASE="${MYSQL_DATABASE:-datasync}"
MYSQL_PID=""
APP_PID=""

export MYSQL_PASSWORD="${MYSQL_PASSWORD:-datasync}"
export MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
export MYSQL_URL="${MYSQL_URL:-jdbc:mysql://127.0.0.1:3306/${MYSQL_DATABASE}}"

mkdir -p "$MYSQL_DATA_DIR" /run/mysqld /sync
chown -R mysql:mysql "$MYSQL_DATA_DIR" /run/mysqld

if [ ! -d "$MYSQL_DATA_DIR/mysql" ]; then
  mariadb-install-db \
    --user=mysql \
    --datadir="$MYSQL_DATA_DIR" \
    --auth-root-authentication-method=normal \
    --skip-test-db
fi

mariadbd \
  --user=mysql \
  --datadir="$MYSQL_DATA_DIR" \
  --socket="$MYSQL_SOCKET" \
  --bind-address=127.0.0.1 \
  --port=3306 \
  --skip-networking=0 &
MYSQL_PID=$!

cleanup() {
  [ -n "$APP_PID" ] && kill "$APP_PID" 2>/dev/null || true
  [ -n "$MYSQL_PID" ] && kill "$MYSQL_PID" 2>/dev/null || true
  [ -n "$APP_PID" ] && wait "$APP_PID" 2>/dev/null || true
  [ -n "$MYSQL_PID" ] && wait "$MYSQL_PID" 2>/dev/null || true
}
trap cleanup INT TERM

for _ in $(seq 1 60); do
  if mysqladmin --protocol=socket --socket="$MYSQL_SOCKET" ping --silent; then
    break
  fi
  sleep 1
done

MYSQL_PASSWORD_SQL="$(printf "%s" "$MYSQL_PASSWORD" | sed "s/'/''/g")"

if mysql --protocol=socket --socket="$MYSQL_SOCKET" -uroot -e "SELECT 1" >/dev/null 2>&1; then
  mysql --protocol=socket --socket="$MYSQL_SOCKET" -uroot <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD_SQL}';
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
SQL
fi

mysql --protocol=socket --socket="$MYSQL_SOCKET" -uroot -p"$MYSQL_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

mysql --protocol=socket --socket="$MYSQL_SOCKET" -uroot -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < /app/mysql-init.sql

java -jar /app/app.jar &
APP_PID=$!
wait "$APP_PID"
