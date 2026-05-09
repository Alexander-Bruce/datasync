package backend.controller;

import backend.service.UserService;
import backend.util.ResultEntity;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/unauthorized")
public class UnAuthController {

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
}
