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
