package backend.controller;

import backend.util.ResultEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/log")
public class LogController {

  private static final String LOG_FILE = "log/log.log";

  @GetMapping("/list")
  public ResponseEntity<ResultEntity<Object>> getLogs(
      @RequestParam(defaultValue = "300") int lines) {
    var path = Paths.get(LOG_FILE);
    if (!Files.exists(path)) {
      return ResultEntity.success(200, "No log file", Collections.emptyList());
    }
    try {
      List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
      int from = Math.max(0, all.size() - lines);
      List<String> result = all.subList(from, all.size());
      return ResultEntity.success(200, "OK", result);
    } catch (IOException e) {
      return ResultEntity.error(500, "读取日志失败: " + e.getMessage());
    }
  }
}
