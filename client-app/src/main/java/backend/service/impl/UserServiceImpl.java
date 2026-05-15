package backend.service.impl;

import backend.exception.model.BaseException;
import backend.mapper.sqlite.UserMapper;
import backend.model.entity.User;
import backend.service.UserService;
import backend.util.HttpJsonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Autowired private UserMapper userMapper;

  @Override
  public User login(String email, String password) {

    Map<String, String> userMap =
        HttpJsonClient.postForData(
            "unauthorized/login",
            Map.of(
                "email", email,
                "password", password),
            new TypeReference<>() {});

    if (userMap == null) throw new BaseException("Please Sign up firstly.", 401);

    User updatedUser =
        User.builder()
            .id(Integer.valueOf(userMap.get("id")))
            .username(userMap.get("username"))
            .email(userMap.get("email"))
            .refreshToken(userMap.get("token"))
            .avatar(userMap.get("avatar"))
            .build();

    User user = userMapper.selectByEmail(email);
    if (user == null) userMapper.insert(updatedUser);
    else userMapper.update(updatedUser);

    return updatedUser;
  }

  @Override
  public User signUp(String username, String email, String password) {

    Map<String, String> userMap =
        HttpJsonClient.postForData(
            "unauthorized/signup",
            Map.of(
                "username", username,
                "email", email,
                "password", password),
            new TypeReference<>() {});

    if (userMap.get("code") != null && Integer.valueOf(userMap.get("code")) == 401)
      throw new BaseException("User already existing.", 402);

    User newUser =
        User.builder()
            .id(Integer.valueOf(userMap.get("id")))
            .username(userMap.get("username"))
            .email(userMap.get("email"))
            .refreshToken(userMap.get("token"))
            .avatar(userMap.get("avatar"))
            .build();

    userMapper.insert(newUser);

    return newUser;
  }

  @Override
  public User update(User user) {
    Map<String, String> userMap =
        HttpJsonClient.postForData("server/user/update", user, new TypeReference<>() {});

    User updatedUser =
        User.builder()
            .id(Integer.valueOf(userMap.get("id")))
            .username(userMap.get("username"))
            .email(userMap.get("email"))
            .avatar(userMap.get("avatar"))
            .userAgent(user.getUserAgent())
            .accessToken(user.getAccessToken())
            .refreshToken(user.getRefreshToken())
            .build();

    userMapper.update(updatedUser);
    return updatedUser;
  }

  @Override
  public User requireLocalSession(Integer id, String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new BaseException("Please login again.", 401);
    }

    User localUser = userMapper.selectByEmail(email.trim());
    if (localUser == null || localUser.getId() == null) {
      throw new BaseException("Please login again.", 401);
    }

    if (id != null && !id.equals(localUser.getId())) {
      throw new BaseException("Please login again.", 401);
    }

    return localUser;
  }
}
