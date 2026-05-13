package backend.controller;

import backend.exception.model.BaseException;
import backend.service.FileService;
import backend.util.ResultEntity;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/sync")
public class SyncController {

  @Autowired private FileService fileService;

  @PostMapping("/upload")
  public ResponseEntity<ResultEntity<Object>> uploadFile(@RequestBody Map<String, String> map) {

    boolean uploaded = fileService.upload(parseFileId(map), map.get("path"), map.get("email"));
    if (!uploaded) {
      throw new BaseException("File sync failed", 500);
    }

    return ResultEntity.success(200, "File sync successfully", true);
  }

  @PostMapping("/download")
  public ResponseEntity<ResultEntity<Object>> downloadFile(@RequestBody Map<String, String> map) {

    boolean downloaded = fileService.download(parseFileId(map), map.get("path"), map.get("email"));
    if (!downloaded) {
      throw new BaseException("File sync failed", 500);
    }

    return ResultEntity.success(200, "File sync successfully", true);
  }

  @PostMapping("/update")
  public ResponseEntity<ResultEntity<Object>> updateSyncTask(@RequestBody Map<String, String> map) {

    System.out.println(map);

    int fileId = parseFileId(map);

    String path = map.get("path");

    String email = map.get("email");

    String scheduled = map.get("scheduled");

    String alias = map.get("alias");

    String alg = map.get("cdcAlg");

    String desc = map.get("description");

    String remoteHost = map.get("remoteHost");

    boolean isDir = Boolean.parseBoolean(map.get("isDir"));

    return ResultEntity.success(
        200,
        "Update Sync Task successfully",
        fileService.updateFileTask(
            fileId, alias, path, email, scheduled, alg, desc, remoteHost, isDir));
  }

  @PostMapping("/delete")
  public ResponseEntity<ResultEntity<Object>> deleteSyncTask(@RequestBody Map<String, String> map) {

    String path = map.get("path");

    String email = map.get("email");

    return ResultEntity.success(
        200,
        "Delete File list successfully",
        fileService.deleteFileTask(parseFileId(map), path, email));
  }

  private int parseFileId(Map<String, String> map) {
    String stringFileId = map.get("fileId");
    if (stringFileId == null || stringFileId.isBlank() || "null".equalsIgnoreCase(stringFileId)) {
      return 0;
    }
    return Integer.parseInt(stringFileId);
  }
}
