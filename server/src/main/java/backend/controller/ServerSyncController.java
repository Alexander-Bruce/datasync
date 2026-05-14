package backend.controller;

import backend.exception.model.BaseException;
import backend.migration.ScopeStorageMigrationRunner;
import backend.service.FileService;
import backend.util.RemoteScope;
import backend.util.ResultEntity;
import backend.util.SyncStyle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("server/file")
public class ServerSyncController {

  @Autowired public FileService fileService;

  @Autowired(required = false)
  private ScopeStorageMigrationRunner migrationRunner;

  @PostMapping("/compare")
  public ResponseEntity<ResultEntity<Object>> compareFile(@RequestBody Map<String, Object> map)
      throws Exception {

    String email = (String) map.get("email");
    String path = (String) map.get("path");
    String scopeName = (String) map.get("scopeName");
    boolean isDir = Boolean.TRUE.equals(map.get("isDir"));

    if (email == null || email.isBlank()) throw new BaseException("email is required", 400);
    if (path == null || path.isBlank()) throw new BaseException("path is required", 400);
    if (scopeName == null || scopeName.isBlank())
      throw new BaseException("scopeName is required", 400);
    Object listObj = map.get("list");
    ObjectMapper objectMapper = new ObjectMapper();

    String json = objectMapper.writeValueAsString(listObj);

    List<SyncStyle> fileList =
        objectMapper.readValue(json, new TypeReference<List<SyncStyle>>() {});

    return ResultEntity.success(
        200,
        "File compare successfully",
        fileService.compare(email, path, scopeName, isDir, fileList));
  }

  /**
   * 下行同步第一步：返回服务端该范围下所有文件的相对路径列表（不含文件内容）。
   *
   * <p>请求体：{ "scopeName": "Work" }
   */
  @PostMapping("/download")
  public ResponseEntity<ResultEntity<Object>> listDownloadFiles(
      @RequestBody Map<String, String> map) {

    String scopeName = map.get("scopeName");
    if (scopeName == null || scopeName.isBlank())
      throw new BaseException("scopeName is required", 400);

    return ResultEntity.success(
        200, "File list fetched successfully", fileService.listDownloadFiles(scopeName));
  }

  /**
   * 下行同步第二步：按相对路径返回单个文件的原始字节（application/octet-stream）。
   *
   * <p>请求体：{ "scopeName": "Work", "relativePath": "subdir/file.txt" }
   */
  @PostMapping("/download/file")
  public ResponseEntity<byte[]> downloadSingleFile(@RequestBody Map<String, String> map) {

    String scopeName = map.get("scopeName");
    String relativePath = map.get("relativePath");

    if (scopeName == null || scopeName.isBlank())
      throw new BaseException("scopeName is required", 400);
    if (relativePath == null || relativePath.isBlank())
      throw new BaseException("relativePath is required", 400);

    byte[] content = fileService.downloadFile(scopeName, relativePath);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(content);
  }

  @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<ResultEntity<Object>> uploadSingleFile(
      @RequestParam String storagePath, @RequestParam String fileName, HttpServletRequest request)
      throws IOException {

    long bytes = fileService.uploadFile(storagePath, fileName, request.getInputStream());

    return ResultEntity.success(200, "File uploaded successfully", Map.of("bytes", bytes));
  }

  /**
   * 列出某用户在 bucket 中已有的全部 scope，供"刚装客户端、本地没有 task"的场景从远端恢复任务列表。
   *
   * <p>请求体：{ "email": "user@example.com" }
   */
  @PostMapping("/list-scopes")
  public ResponseEntity<ResultEntity<Object>> listUserScopes(@RequestBody Map<String, String> map) {
    String email = map.get("email");
    if (email == null || email.isBlank()) throw new BaseException("email is required", 400);

    List<RemoteScope> scopes = fileService.listUserScopes(email);
    return ResultEntity.success(200, "User scopes fetched", scopes);
  }

  /**
   * 手动触发一次 legacy storage 迁移。startup 阶段已经自动跑过一次；这个端点用于「迁移因为底层桶慢/异常没完成」时安全重跑，
   * 因为迁移本身是幂等的，重复跑只会处理上次没搬完的剩余条目。
   */
  @PostMapping("/migrate-legacy-storage")
  public ResponseEntity<ResultEntity<Object>> migrateLegacyStorage() throws IOException {
    if (migrationRunner == null) {
      throw new BaseException("Migration runner not active (non-docker profile)", 409);
    }
    ScopeStorageMigrationRunner.MigrationReport report = migrationRunner.migrateAll();
    return ResultEntity.success(
        200,
        "Migration finished",
        Map.of(
            "scanned",
            report.scanned,
            "migrated",
            report.migrated,
            "skipped",
            report.skipped,
            "failed",
            report.failed,
            "groupScopes",
            report.groupScopes));
  }
}
