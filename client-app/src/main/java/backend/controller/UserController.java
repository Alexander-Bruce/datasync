package backend.controller;

import backend.model.entity.User;
import backend.service.UserService;
import backend.util.HttpJsonClient;
import backend.util.ResultEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/client/user")
public class UserController {

  @Autowired private UserService userService;

  @PostMapping("/login")
  public ResponseEntity<ResultEntity<Object>> login(@RequestBody Map<String, String> userInfo) {

    String email = userInfo.get("email");

    String password = userInfo.get("password");

    return ResultEntity.success(200, "Login successfully", userService.login(email, password));
  }

  @PostMapping("/signup")
  public ResponseEntity<ResultEntity<Object>> signUp(@RequestBody Map<String, String> userInfo) {

    String email = userInfo.get("email");

    String password = userInfo.get("password");

    String username = userInfo.get("username");

    return ResultEntity.success(
        200, "Sign up successfully", userService.signUp(username, email, password));
  }

  @PostMapping("/update")
  public ResponseEntity<ResultEntity<Object>> update(@RequestBody User user) {

    return ResultEntity.success(200, "User updated", userService.update(user));
  }

  @PostMapping("/session")
  public ResponseEntity<ResultEntity<Object>> session(@RequestBody Map<String, String> userInfo) {
    String stringId = userInfo.get("id");
    Integer id =
        stringId == null || stringId.isBlank() || "null".equalsIgnoreCase(stringId)
            ? null
            : Integer.valueOf(stringId);
    return ResultEntity.success(
        200, "Local session verified", userService.requireLocalSession(id, userInfo.get("email")));
  }

  @PostMapping("/session/current")
  public ResponseEntity<ResultEntity<Object>> currentSession() {
    return ResultEntity.success(200, "Local session restored", userService.getCachedSession());
  }

  @PostMapping("/search")
  public ResponseEntity<ResultEntity<Object>> searchUsers(@RequestBody Map<String, String> map) {
    Object data =
        HttpJsonClient.postForData("server/user/search", map, new TypeReference<Object>() {});
    return ResultEntity.success(200, "Users found", data);
  }
}
