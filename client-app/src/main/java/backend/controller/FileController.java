package backend.controller;

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
}
