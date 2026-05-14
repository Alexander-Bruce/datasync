package backend.controller;

import backend.service.FileService;
import backend.util.HttpJsonClient;
import backend.util.ResultEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/client/file")
public class FileController {

  @Autowired public FileService fileService;

  @PostMapping("/brief-list")
  public ResponseEntity<ResultEntity<Object>> getBriefFileList(
      @RequestBody Map<String, String> map) {

    String email = map.get("email");

    System.out.println(email);

    return ResultEntity.success(
        200, "Get brief fileList successfully", fileService.getBriefFileList(email));
  }

  @PostMapping("/delete")
  public ResponseEntity<ResultEntity<Object>> getDeleteFile(@RequestBody Map<String, String> map) {

    String email = map.get("email");

    String path = map.get("path");

    return ResultEntity.success(
        200, "Delete file successfully", fileService.deleteFile(email, path));
  }

  @PostMapping("/detail-list")
  public ResponseEntity<ResultEntity<Object>> getDetailedFileList(
      @RequestBody Map<String, String> map) {

    int fileId = Integer.parseInt(map.get("fileId"));

    return ResultEntity.success(
        200, "Get fileList details successfully", fileService.getDetailedFileList(fileId));
  }

  @PostMapping("/detail-list-parent")
  public ResponseEntity<ResultEntity<Object>> getDetailedFileListByParent(
      @RequestBody Map<String, String> map) {

    int fileId = Integer.parseInt(map.get("fileId"));

    return ResultEntity.success(
        200, "Get fileList details successfully", fileService.getDetailedFileListByParent(fileId));
  }

  /**
   * 列出该 email 在远端 bucket 已有的所有 scope，给"刚装好客户端、本地数据库还空着"的场景用。 客户端不写入本地数据库，UI
   * 拿到列表后可以让用户选择要恢复哪些任务、放到本地哪个目录。 透传到 server 的 /file/list-scopes 端点。
   */
  @PostMapping("/remote-scopes")
  public ResponseEntity<ResultEntity<Object>> getRemoteScopes(
      @RequestBody Map<String, String> map) {
    String email = map.get("email");
    List<Map<String, Object>> scopes =
        HttpJsonClient.postForData(
            "server/file/list-scopes", Map.of("email", email), new TypeReference<>() {});
    return ResultEntity.success(200, "Remote scopes fetched", scopes);
  }
}
