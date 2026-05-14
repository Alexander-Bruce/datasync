package backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.util.SyncStyle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class FileServiceImplTest {

  @TempDir Path tempDir;

  @Test
  void listAndDownloadSingleFileScope() throws Exception {
    FileServiceImpl service = serviceWithTempStorage();
    Path remoteFile = tempDir.resolve("user@example.com").resolve("Task A").resolve("note.txt");
    Files.createDirectories(remoteFile.getParent());
    Files.writeString(remoteFile, "hello", StandardCharsets.UTF_8);

    List<String> files = service.listDownloadFiles("user@example.com/Task A/note.txt");

    assertEquals(List.of("note.txt"), files);
    assertArrayEquals(
        "hello".getBytes(StandardCharsets.UTF_8),
        service.downloadFile("user@example.com/Task A/note.txt", "note.txt"));
  }

  @Test
  void listUserScopesReportsAliasRootNameAndIsDir() throws Exception {
    FileServiceImpl service = serviceWithTempStorage();
    // 新布局：一个文件夹 scope + 一个单文件 scope
    Path emailDir = tempDir.resolve("alice@example.com");
    Files.createDirectories(emailDir.resolve("Work").resolve("Docs"));
    Files.writeString(
        emailDir.resolve("Work").resolve("Docs").resolve("a.txt"), "x", StandardCharsets.UTF_8);
    Files.createDirectories(emailDir.resolve("MyNote"));
    Files.writeString(emailDir.resolve("MyNote").resolve("MyNote"), "n", StandardCharsets.UTF_8);

    List<backend.util.RemoteScope> scopes = service.listUserScopes("alice@example.com");

    assertEquals(2, scopes.size());
    assertEquals("MyNote", scopes.get(0).alias);
    assertEquals("MyNote", scopes.get(0).rootName);
    assertFalse(scopes.get(0).isDir);
    assertEquals("alice@example.com/MyNote/MyNote", scopes.get(0).scopeName);

    assertEquals("Work", scopes.get(1).alias);
    assertEquals("Docs", scopes.get(1).rootName);
    assertTrue(scopes.get(1).isDir);
    assertEquals("alice@example.com/Work/Docs", scopes.get(1).scopeName);
  }

  @Test
  void compareUsesNewAliasScopeWithoutTouchingOldEmailRootPath() throws Exception {
    FileServiceImpl service = serviceWithTempStorage();
    Path oldFile = tempDir.resolve("user@example.com").resolve("Documents").resolve("old.txt");
    Path newFile =
        tempDir
            .resolve("user@example.com")
            .resolve("Task A")
            .resolve("Documents")
            .resolve("old.txt");
    Path staleFile =
        tempDir
            .resolve("user@example.com")
            .resolve("Task A")
            .resolve("Documents")
            .resolve("stale.txt");
    Files.createDirectories(oldFile.getParent());
    Files.createDirectories(newFile.getParent());
    Files.writeString(oldFile, "old", StandardCharsets.UTF_8);
    Files.writeString(newFile, "new", StandardCharsets.UTF_8);
    Files.writeString(staleFile, "stale", StandardCharsets.UTF_8);

    SyncStyle style = new SyncStyle();
    style.file = Path.of("old.txt").toFile();
    style.storagePath = "user@example.com/Task A/Documents";

    service.compare(
        "user@example.com",
        "/local/Documents",
        "user@example.com/Task A/Documents",
        true,
        List.of(style));

    assertFalse(Files.exists(staleFile));
    assertTrue(Files.exists(oldFile));
    assertEquals("old", Files.readString(oldFile, StandardCharsets.UTF_8));
    assertEquals("new", Files.readString(newFile, StandardCharsets.UTF_8));
  }

  private FileServiceImpl serviceWithTempStorage() {
    FileServiceImpl service = new FileServiceImpl();
    ReflectionTestUtils.setField(service, "basePath", tempDir.toString());
    return service;
  }
}
