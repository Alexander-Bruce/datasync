package backend.controller;

import backend.service.FileService;
import backend.util.HttpJsonClient;
import backend.util.ResultEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/group")
public class GroupController {

  @Autowired private FileService fileService;

  @PostMapping("/create")
  public ResponseEntity<ResultEntity<Object>> createGroup(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/create", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Group created", data);
  }

  @PostMapping("/add-member")
  public ResponseEntity<ResultEntity<Object>> addMember(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/add-member", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Member added", data);
  }

  @PostMapping("/remove-member")
  public ResponseEntity<ResultEntity<Object>> removeMember(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData(
            "server/group/remove-member", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Member removed", data);
  }

  @PostMapping("/add-scope")
  public ResponseEntity<ResultEntity<Object>> addScope(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/add-scope", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Scope added", data);
  }

  @PostMapping("/remove-scope")
  public ResponseEntity<ResultEntity<Object>> removeScope(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData(
            "server/group/remove-scope", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Scope removed", data);
  }

  @PostMapping("/delete")
  public ResponseEntity<ResultEntity<Object>> deleteGroup(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/delete", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Group deleted", data);
  }

  @PostMapping("/list")
  public ResponseEntity<ResultEntity<Object>> listGroups(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/list", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Groups fetched", data);
  }

  @PostMapping("/files")
  public ResponseEntity<ResultEntity<Object>> getGroupFiles(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/files", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Group files fetched", data);
  }

  @PostMapping("/add-admin")
  public ResponseEntity<ResultEntity<Object>> addAdmin(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/group/add-admin", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Admin added", data);
  }

  @PostMapping("/remove-admin")
  public ResponseEntity<ResultEntity<Object>> removeAdmin(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData(
            "server/group/remove-admin", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Admin removed", data);
  }

  @PostMapping("/add-members")
  public ResponseEntity<ResultEntity<Object>> addMembers(@RequestBody Map<String, Object> map) {
    Object data =
        HttpJsonClient.postForData("server/group/add-members", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Members added", data);
  }

  @PostMapping("/remove-members")
  public ResponseEntity<ResultEntity<Object>> removeMembers(@RequestBody Map<String, Object> map) {
    Object data =
        HttpJsonClient.postForData(
            "server/group/remove-members", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Members removed", data);
  }

  /** 异步下载 group scope 到本地路径，立即返回，后台执行。 Body: { email, scopeName, localPath } */
  @PostMapping("/download-scope")
  public ResponseEntity<ResultEntity<Object>> downloadScope(@RequestBody Map<String, String> map) {
    String scopeName = map.get("scopeName");
    String localPath = map.get("localPath");
    String relativePath = map.get("relativePath");

    CompletableFuture.runAsync(() -> fileService.downloadScope(scopeName, localPath, relativePath));

    return ResultEntity.success(200, "Download started", null);
  }
}
