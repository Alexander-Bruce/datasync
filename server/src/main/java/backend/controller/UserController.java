package backend.controller;

import backend.exception.model.BaseException;
import backend.model.entity.User;
import backend.service.UserService;
import backend.util.ResultEntity;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/user")
public class UserController {

  @Autowired private UserService userService;

  @PostMapping("/search")
  public ResponseEntity<ResultEntity<Object>> searchUsers(@RequestBody Map<String, String> map) {
    return ResultEntity.success(200, "Users found", userService.searchUsers(map.get("q")));
  }

  @PostMapping("/resolve")
  public ResponseEntity<ResultEntity<Object>> resolveUser(@RequestBody Map<String, String> map) {
    return ResultEntity.success(200, "User resolved", userService.resolveUser(map.get("email")));
  }

  @PostMapping("/update")
  public ResponseEntity<ResultEntity<Object>> update(@RequestBody User user) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User authUser) {
      if (!authUser.getId().equals(user.getId())) {
        throw new BaseException("Permission denied: cannot update another user's profile", 403);
      }
    }
    return ResultEntity.success(200, "User updated", userService.update(user));
  }
}
