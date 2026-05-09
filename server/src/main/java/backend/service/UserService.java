package backend.service;

import backend.model.entity.User;
import java.util.List;
import java.util.Map;

public interface UserService {
  Map<String, String> login(String username, String password);

  Map<String, String> signUp(String username, String email, String password);

  Map<String, String> update(User user);

  List<Map<String, String>> searchUsers(String query);
}
