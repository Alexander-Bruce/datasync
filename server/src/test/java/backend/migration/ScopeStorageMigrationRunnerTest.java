package backend.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.model.Group;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

class ScopeStorageMigrationRunnerTest {

  @TempDir Path tempDir;

  @Test
  void migratesLegacyFolderAndFileScopesIntoAliasLayout() throws Exception {
    // 旧布局: basePath/email/<X>，新布局: basePath/email/<X>/<X>
    Path emailDir = tempDir.resolve("user@example.com");
    Files.createDirectories(emailDir.resolve("Docs"));
    Files.writeString(emailDir.resolve("Docs").resolve("note.txt"), "doc", StandardCharsets.UTF_8);
    Files.writeString(emailDir.resolve("only.txt"), "single", StandardCharsets.UTF_8);

    runMigration();

    assertTrue(Files.isDirectory(emailDir.resolve("Docs").resolve("Docs")));
    assertEquals(
        "doc",
        Files.readString(
            emailDir.resolve("Docs").resolve("Docs").resolve("note.txt"), StandardCharsets.UTF_8));
    assertTrue(Files.isDirectory(emailDir.resolve("only.txt")));
    assertEquals(
        "single",
        Files.readString(emailDir.resolve("only.txt").resolve("only.txt"), StandardCharsets.UTF_8));
  }

  @Test
  void migrationIsIdempotent() throws Exception {
    Path emailDir = tempDir.resolve("user@example.com");
    Files.createDirectories(emailDir.resolve("Pictures"));
    Files.writeString(
        emailDir.resolve("Pictures").resolve("a.jpg"), "bytes", StandardCharsets.UTF_8);

    runMigration();
    // 第二次运行不应再次嵌套（不会出现 Pictures/Pictures/Pictures）
    runMigration();

    assertTrue(Files.exists(emailDir.resolve("Pictures").resolve("Pictures").resolve("a.jpg")));
    assertFalse(Files.exists(emailDir.resolve("Pictures").resolve("Pictures").resolve("Pictures")));
  }

  @Test
  void migrationCoversPrivateScopesNotListedInGroupsJson() throws Exception {
    Path emailA = tempDir.resolve("alice@example.com");
    Path emailB = tempDir.resolve("bob@example.com");
    Files.createDirectories(emailA.resolve("Shared"));
    Files.writeString(emailA.resolve("Shared").resolve("x.txt"), "x", StandardCharsets.UTF_8);
    Files.createDirectories(emailB.resolve("PrivateOnly"));
    Files.writeString(emailB.resolve("PrivateOnly").resolve("y.txt"), "y", StandardCharsets.UTF_8);

    // groups.json 只引用了 alice 的 Shared，bob 的 PrivateOnly 不在群组中
    Group group =
        Group.builder()
            .id("g1")
            .name("Shared")
            .ownerEmail("alice@example.com")
            .admins(new ArrayList<>())
            .members(new ArrayList<>())
            .scopes(new ArrayList<>(List.of("alice@example.com/Shared")))
            .build();
    new ObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValue(tempDir.resolve("groups.json").toFile(), List.of(group));

    runMigration();

    // 两个 scope 在桶里都应迁移到新布局
    assertTrue(Files.exists(emailA.resolve("Shared").resolve("Shared").resolve("x.txt")));
    assertTrue(Files.exists(emailB.resolve("PrivateOnly").resolve("PrivateOnly").resolve("y.txt")));

    // groups.json 也跟随更新
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> updated =
        new ObjectMapper().readValue(tempDir.resolve("groups.json").toFile(), List.class);
    @SuppressWarnings("unchecked")
    List<String> scopes = (List<String>) updated.get(0).get("scopes");
    assertEquals(List.of("alice@example.com/Shared/Shared"), scopes);
  }

  private void runMigration() {
    ScopeStorageMigrationRunner runner = new ScopeStorageMigrationRunner();
    ReflectionTestUtils.setField(runner, "basePath", tempDir.toString());
    runner.run(new DefaultApplicationArguments());
  }
}
