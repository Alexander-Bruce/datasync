package backend.controller;

import backend.config.ClientConfigStore;
import backend.model.ClientConfig;
import backend.util.HttpJsonClient;
import backend.util.ResultEntity;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/config")
public class ClientConfigController {

  @GetMapping
  public ResponseEntity<ResultEntity<ClientConfig>> getConfig() {
    return ResultEntity.success(200, "Client config loaded", ClientConfigStore.load());
  }

  @PostMapping
  public ResponseEntity<ResultEntity<ClientConfig>> saveConfig(@RequestBody ClientConfig config) {
    return ResultEntity.success(200, "Client config saved", ClientConfigStore.save(config));
  }

  @PostMapping("/test")
  public ResponseEntity<ResultEntity<Map<String, Object>>> testConfig(
      @RequestBody ClientConfig config) {
    return ResultEntity.success(
        200, "Remote server reached", HttpJsonClient.testConnection(config));
  }
}
