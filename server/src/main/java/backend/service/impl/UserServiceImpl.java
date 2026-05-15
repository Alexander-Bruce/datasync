package backend.service.impl;

import backend.exception.model.BaseException;
import backend.exception.model.LoginFailedException;
import backend.interceptor.JwtService;
import backend.mapper.mysql.UserMapper;
import backend.model.entity.User;
import backend.service.UserService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class UserServiceImpl implements UserService {

  @Autowired private UserMapper userMapper;

  @Autowired private BCryptPasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @Value("${spring.netty.server.basePath}")
  private String basePath;

  @Value("${application.server.public-base-url:${PUBLIC_BASE_URL:}}")
  private String publicBaseUrl;

  @Override
  public Map<String, String> login(String email, String password) {

    User user = userMapper.selectByEmail(email);

    try {

      if (user == null || !passwordEncoder.matches(password, user.getPassword()))
        throw new LoginFailedException();

      String jwt = jwtService.generateToken(user.getId());

      Map<String, String> map = new HashMap<>();
      map.put("token", jwt);
      map.put("id", String.valueOf(user.getId()));
      map.put("username", user.getUsername());
      map.put("email", user.getEmail());
      map.put("avatar", publicAvatar(user.getAvatar()));

      return map;
    } catch (LoginFailedException ex) {
      throw new LoginFailedException();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public Map<String, String> signUp(String username, String email, String password) {
    User user = userMapper.selectByEmail(email);

    try {
      if (user != null && !passwordEncoder.matches(password, user.getPassword()))
        throw new LoginFailedException();

      if (user != null) return login(email, password);

      User u =
          User.builder()
              .username(username)
              .email(email)
              .password(passwordEncoder.encode(password))
              .build();

      userMapper.insert(u);

      String jwt = jwtService.generateToken(u.getId());

      Map<String, String> map = new HashMap<>();
      map.put("token", jwt);
      map.put("id", String.valueOf(u.getId()));
      map.put("username", u.getUsername());
      map.put("email", u.getEmail());
      map.put("avatar", publicAvatar(u.getAvatar()));

      return map;
    } catch (LoginFailedException ex) {
      throw new LoginFailedException();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public Map<String, String> update(User user) {
    if (user.getId() == null) {
      throw new BaseException("User id is required", 400);
    }

    User existing = userMapper.selectById(user.getId());
    if (existing == null) {
      throw new BaseException("User not found", 404);
    }

    User updatedUser =
        User.builder()
            .id(existing.getId())
            .username(firstNonBlank(user.getUsername(), existing.getUsername()))
            .password(existing.getPassword())
            .email(firstNonBlank(user.getEmail(), existing.getEmail()))
            .avatar(resolveAvatar(user.getAvatar(), existing.getAvatar(), existing.getId()))
            .build();

    userMapper.update(updatedUser);

    Map<String, String> map = new HashMap<>();
    map.put("id", String.valueOf(updatedUser.getId()));
    map.put("username", updatedUser.getUsername());
    map.put("email", updatedUser.getEmail());
    map.put("avatar", publicAvatar(updatedUser.getAvatar()));
    return map;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.trim().isEmpty() ? fallback : value.trim();
  }

  private String resolveAvatar(String avatar, String existingAvatar, Integer userId) {
    if (avatar == null) return existingAvatar;
    String trimmed = avatar.trim();
    if (trimmed.isEmpty()) return "";
    if (!trimmed.startsWith("data:image/")) return trimmed;

    int comma = trimmed.indexOf(',');
    int semicolon = trimmed.indexOf(';');
    if (comma < 0
        || semicolon < 0
        || semicolon > comma
        || !trimmed.substring(semicolon, comma).contains("base64")) {
      throw new BaseException("Invalid avatar image data", 400);
    }

    String mime = trimmed.substring("data:".length(), semicolon);
    String extension =
        switch (mime) {
          case "image/png" -> "png";
          case "image/jpeg" -> "jpg";
          case "image/webp" -> "webp";
          case "image/gif" -> "gif";
          default -> throw new BaseException("Unsupported avatar image type", 400);
        };

    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(trimmed.substring(comma + 1));
    } catch (IllegalArgumentException ex) {
      throw new BaseException("Invalid avatar image data", 400);
    }
    if (bytes.length > 2 * 1024 * 1024) {
      throw new BaseException("Avatar image must be 2MB or smaller", 400);
    }

    File avatarDir = new File(basePath, "avatars");
    if (!avatarDir.exists() && !avatarDir.mkdirs()) {
      throw new BaseException("Failed to create avatar storage", 500);
    }
    File avatarFile = new File(avatarDir, userId + "." + extension);
    try {
      Files.write(avatarFile.toPath(), bytes);
    } catch (IOException ex) {
      throw new BaseException("Failed to save avatar image", 500);
    }
    return buildPublicUrl("/resources/avatars/" + avatarFile.getName(), System.currentTimeMillis());
  }

  private String buildPublicUrl(String path, long version) {
    String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    if (base.isEmpty()) {
      try {
        base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
      } catch (IllegalStateException ex) {
        base = "";
      }
    }
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base + path + "?v=" + version;
  }

  private String publicAvatar(String avatar) {
    if (avatar == null || avatar.trim().isEmpty()) return avatar;
    String trimmed = avatar.trim();
    int resourceIndex = trimmed.indexOf("/resources/avatars/");
    if (resourceIndex < 0) return trimmed;
    String path = trimmed.substring(resourceIndex);
    int queryIndex = path.indexOf('?');
    if (queryIndex >= 0) path = path.substring(0, queryIndex);
    return buildPublicUrl(path, System.currentTimeMillis());
  }

  @Override
  public List<Map<String, String>> searchUsers(String query) {
    if (query == null || query.trim().isEmpty()) return new ArrayList<>();
    List<User> users = userMapper.searchByQuery(query.trim());
    List<Map<String, String>> result = new ArrayList<>();
    for (User u : users) {
      Map<String, String> m = new HashMap<>();
      m.put("email", u.getEmail());
      m.put("username", u.getUsername() != null ? u.getUsername() : "");
      String avatar = publicAvatar(u.getAvatar());
      m.put("avatar", avatar != null ? avatar : "");
      result.add(m);
    }
    return result;
  }

  @Override
  public Map<String, String> resolveUser(String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new BaseException("email is required", 400);
    }

    User user = userMapper.selectByEmail(email.trim());
    if (user == null) {
      throw new BaseException("User not found", 404);
    }

    Map<String, String> map = new HashMap<>();
    map.put("id", String.valueOf(user.getId()));
    map.put("username", user.getUsername() != null ? user.getUsername() : "");
    map.put("email", user.getEmail());
    String avatar = publicAvatar(user.getAvatar());
    map.put("avatar", avatar != null ? avatar : "");
    return map;
  }
}
