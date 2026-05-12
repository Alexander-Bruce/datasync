package backend.config;

import backend.exception.model.BaseException;
import backend.model.ClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ClientConfigStore {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DEFAULT_SYNC_PORT = 8443;
  private static final Path CONFIG_PATH =
      Paths.get(System.getProperty("user.home"), ".datasync", "client-config.json");

  private ClientConfigStore() {}

  public static synchronized ClientConfig load() {
    if (!Files.exists(CONFIG_PATH)) {
      return empty();
    }

    try {
      ClientConfig config = OBJECT_MAPPER.readValue(CONFIG_PATH.toFile(), ClientConfig.class);
      return normalize(config, false);
    } catch (IOException | IllegalArgumentException ex) {
      return empty();
    }
  }

  public static synchronized ClientConfig save(ClientConfig config) {
    ClientConfig normalized = normalize(config, true);
    try {
      Files.createDirectories(CONFIG_PATH.getParent());
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(CONFIG_PATH.toFile(), normalized);
      return normalized;
    } catch (IOException ex) {
      throw new BaseException("Failed to write client configuration: " + ex.getMessage(), 500);
    }
  }

  public static ClientConfig requireConfigured() {
    ClientConfig config = load();
    if (!config.isConfigured()) {
      throw new BaseException("Remote server is not configured. Open setup first.", 428);
    }
    return config;
  }

  public static ClientConfig normalize(ClientConfig config, boolean strict) {
    if (config == null) {
      if (strict) {
        throw new BaseException("Configuration payload is required.", 400);
      }
      return empty();
    }

    String serverBaseUrl = normalizeServerBaseUrl(config.getServerBaseUrl(), strict);
    String syncHost = trimToNull(config.getSyncHost());
    Integer syncPort = config.getSyncPort();

    if (serverBaseUrl != null && syncHost == null) {
      syncHost = URI.create(serverBaseUrl).getHost();
    }

    if (syncPort == null) {
      syncPort = DEFAULT_SYNC_PORT;
    }

    if (syncPort <= 0 || syncPort > 65535) {
      throw new BaseException("Sync port must be between 1 and 65535.", 400);
    }

    boolean configured = serverBaseUrl != null && syncHost != null;
    if (strict && !configured) {
      throw new BaseException("Server URL and sync host are required.", 400);
    }

    return new ClientConfig(serverBaseUrl, syncHost, syncPort, configured);
  }

  public static Path getConfigPath() {
    return CONFIG_PATH;
  }

  private static ClientConfig empty() {
    return new ClientConfig(null, null, DEFAULT_SYNC_PORT, false);
  }

  private static String normalizeServerBaseUrl(String value, boolean strict) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      if (strict) {
        throw new BaseException("Server URL is required.", 400);
      }
      return null;
    }

    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      trimmed = "http://" + trimmed;
    }

    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (IllegalArgumentException ex) {
      throw new BaseException("Server URL is invalid.", 400);
    }

    if (uri.getHost() == null || uri.getScheme() == null) {
      throw new BaseException("Server URL must include a valid host.", 400);
    }

    StringBuilder normalized = new StringBuilder();
    normalized.append(uri.getScheme()).append("://").append(uri.getAuthority());
    String path = trimTrailingSlash(uri.getPath());
    if (path != null && !path.isBlank()) {
      normalized.append(path);
    }
    return normalized.toString();
  }

  private static String trimTrailingSlash(String value) {
    String trimmed = trimToNull(value);
    if (trimmed == null || "/".equals(trimmed)) {
      return null;
    }
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
