package backend.service;

import backend.model.entity.User;

public interface UserService {
  User login(String username, String password);

  User signUp(String username, String email, String password);

  User update(User user);

  User requireLocalSession(Integer id, String email);

  User getCachedSession();
}
