package backend.controller;

import backend.model.entity.User;
import backend.service.UserService;
import backend.util.ResultEntity;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/user")
public class UserController {

  @Autowired private UserService userService;

  @PostMapping("/search")
  public ResponseEntity<ResultEntity<Object>> searchUsers(@RequestBody Map<String, String> map) {
    return ResultEntity.success(200, "Users found", userService.searchUsers(map.get("q")));
  }

  @PostMapping("/update")
  public ResponseEntity<ResultEntity<Object>> update(@RequestBody User user) {
    return ResultEntity.success(200, "User updated", userService.update(user));
  }
}
