package backend.datasource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class SqliteSchemaInitializerTest {

  @TempDir Path tempDir;

  @Test
  void initializerCreatesUserIdAvatarColumnsAndUniqueEmailIndex() throws Exception {
    SQLiteDataSource dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("test-user.db"));

    new SqliteSchemaInitializer().ensureSqliteUserColumns(dataSource).run(null);

    try (Connection connection = dataSource.getConnection()) {
      assertTrue(columnNames(connection).contains("id"));
      assertTrue(columnNames(connection).contains("avatar"));
      assertTrue(uniqueIndexes(connection).contains("idx_user_email_unique"));
    }
  }

  private Set<String> columnNames(Connection connection) throws Exception {
    Set<String> columns = new HashSet<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("PRAGMA table_info(User)")) {
      while (rs.next()) {
        columns.add(rs.getString("name"));
      }
    }
    return columns;
  }

  private Set<String> uniqueIndexes(Connection connection) throws Exception {
    Set<String> indexes = new HashSet<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("PRAGMA index_list(User)")) {
      while (rs.next()) {
        if (rs.getInt("unique") == 1) {
          indexes.add(rs.getString("name"));
        }
      }
    }
    return indexes;
  }
}
