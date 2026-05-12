package backend.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqliteSchemaInitializer {

  @Bean
  public ApplicationRunner ensureSqliteSchema(
      @Qualifier("sqliteDataSource") DataSource dataSource) {
    return args -> {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.executeUpdate("PRAGMA foreign_keys = ON");
        statement.executeUpdate(
            """
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
            )
            """);
        statement.executeUpdate(
            """
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
            )
            """);
        statement.executeUpdate(
            """
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
            )
            """);

        Set<String> userColumns = getColumns(connection, "User");
        if (!userColumns.contains("id")) {
          statement.executeUpdate("ALTER TABLE User ADD COLUMN id INTEGER");
        }
        if (!userColumns.contains("avatar")) {
          statement.executeUpdate("ALTER TABLE User ADD COLUMN avatar TEXT");
        }
        statement.executeUpdate(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique ON User(email)");

        Set<String> fileColumns = getColumns(connection, "File");
        addColumnIfMissing(
            statement, fileColumns, "remote_host", "ALTER TABLE File ADD COLUMN remote_host TEXT");
        addColumnIfMissing(
            statement, fileColumns, "user_id", "ALTER TABLE File ADD COLUMN user_id INTEGER");
        addColumnIfMissing(
            statement, fileColumns, "update_time", "ALTER TABLE File ADD COLUMN update_time TEXT");
        addColumnIfMissing(
            statement, fileColumns, "description", "ALTER TABLE File ADD COLUMN description TEXT");
        addColumnIfMissing(
            statement,
            fileColumns,
            "is_dir",
            "ALTER TABLE File ADD COLUMN is_dir BOOLEAN DEFAULT 0");
        addColumnIfMissing(
            statement,
            fileColumns,
            "is_sync",
            "ALTER TABLE File ADD COLUMN is_sync BOOLEAN DEFAULT 0");

        Set<String> subFileColumns = getColumns(connection, "SubFile");
        addColumnIfMissing(
            statement, subFileColumns, "parent", "ALTER TABLE SubFile ADD COLUMN parent INTEGER");
        addColumnIfMissing(
            statement, subFileColumns, "name", "ALTER TABLE SubFile ADD COLUMN name TEXT");
        addColumnIfMissing(
            statement, subFileColumns, "depth", "ALTER TABLE SubFile ADD COLUMN depth INTEGER");
        addColumnIfMissing(
            statement,
            subFileColumns,
            "is_dir",
            "ALTER TABLE SubFile ADD COLUMN is_dir BOOLEAN DEFAULT 0");
        addColumnIfMissing(
            statement,
            subFileColumns,
            "is_sync",
            "ALTER TABLE SubFile ADD COLUMN is_sync BOOLEAN DEFAULT 0");

        statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_file_user_id ON File(user_id)");
        statement.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_subfile_file_id ON SubFile(file_id)");
      }
    };
  }

  private Set<String> getColumns(Connection connection, String tableName) throws SQLException {
    Set<String> columns = new HashSet<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
      while (rs.next()) {
        columns.add(rs.getString("name"));
      }
    }
    return columns;
  }

  private void addColumnIfMissing(
      Statement statement, Set<String> columns, String columnName, String alterSql)
      throws SQLException {
    if (!columns.contains(columnName)) {
      statement.executeUpdate(alterSql);
    }
  }
}
