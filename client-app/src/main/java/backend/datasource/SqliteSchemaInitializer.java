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
  public ApplicationRunner ensureSqliteUserColumns(
      @Qualifier("sqliteDataSource") DataSource dataSource) {
    return args -> {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
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
        Set<String> userColumns = getColumns(connection, "User");
        if (!userColumns.contains("id")) {
          statement.executeUpdate("ALTER TABLE User ADD COLUMN id INTEGER");
        }
        if (!userColumns.contains("avatar")) {
          statement.executeUpdate("ALTER TABLE User ADD COLUMN avatar TEXT");
        }
        statement.executeUpdate(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique ON User(email)");
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
}
