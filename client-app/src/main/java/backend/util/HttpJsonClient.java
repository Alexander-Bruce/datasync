package backend.util;

import backend.config.ClientConfigStore;
import backend.exception.model.BaseException;
import backend.model.ClientConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class HttpJsonClient {

  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private HttpJsonClient() {
    // 私有构造函数，防止实例化
  }

  /**
   * POST JSON 请求，返回 T
   *
   * @param url 请求地址，必须是完整 URL
   * @param body 请求对象，会序列化为 JSON
   * @param headers 请求头，可为 null
   * @param typeRef 期望返回的泛型类型
   * @param <T> 返回数据类型
   * @return 返回 T，如果失败会抛出 RuntimeException
   */
  public static <T> T postForData(
      String url, Object body, Map<String, String> headers, TypeReference<T> typeRef) {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(body);

      url = resolveUrl(url);

      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .header("Content-Type", "application/json");

      if (headers != null) {
        headers.forEach(builder::header);
      }

      HttpRequest request = builder.build();
      HttpResponse<String> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BaseException(
            "HTTP请求失败: " + response.statusCode() + " - " + response.body(), response.statusCode());
      }

      ResultEntity<?> tempResult =
          OBJECT_MAPPER.readValue(response.body(), new TypeReference<ResultEntity<?>>() {});

      if (tempResult.getCode() < 200 || tempResult.getCode() >= 300) {
        throw new BaseException("请求失败: " + tempResult.getMessage(), response.statusCode());
      }

      String dataJson = OBJECT_MAPPER.writeValueAsString(tempResult.getData());
      return OBJECT_MAPPER.readValue(dataJson, typeRef);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("请求异常: " + e.getMessage(), e);
    }
  }

  /** 简化版 POST，不传 headers */
  public static <T> T postForData(String url, Object body, TypeReference<T> typeRef) {
    return postForData(url, body, null, typeRef);
  }

  /**
   * 下载专用：POST JSON 请求，直接返回响应体的原始字节（不解析 JSON）。 不设请求超时，仅靠连接超时检测服务端是否可达。
   *
   * @param url 相对路径（自动拼接 basePath）
   * @param body 请求体对象，会序列化为 JSON
   * @return 响应体的原始字节数组
   */
  public static byte[] downloadBytes(String url, Object body) {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(body);
      String fullUrl = resolveUrl(url);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(fullUrl))
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<byte[]> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new RuntimeException("下载文件失败: HTTP " + response.statusCode());
      }

      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("下载文件异常: " + e.getMessage(), e);
    }
  }

  public static void uploadFile(String url, File file, Map<String, String> params) {
    try {
      String fullUrl = appendQuery(resolveUrl(url), params);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(fullUrl))
              .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
              .header("Content-Type", "application/octet-stream")
              .build();

      HttpResponse<String> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BaseException(
            "HTTP文件上传失败: " + response.statusCode() + " - " + response.body(),
            response.statusCode());
      }

      ResultEntity<?> tempResult =
          OBJECT_MAPPER.readValue(response.body(), new TypeReference<ResultEntity<?>>() {});

      if (tempResult.getCode() < 200 || tempResult.getCode() >= 300) {
        throw new BaseException("文件上传失败: " + tempResult.getMessage(), response.statusCode());
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RuntimeException("文件上传异常: " + e.getMessage(), e);
    }
  }

  public static Map<String, Object> testConnection(ClientConfig config) {
    ClientConfig normalized = ClientConfigStore.normalize(config, true);
    String testUrl = joinUrl(normalized.getServerBaseUrl(), "/");

    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(testUrl))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();

      HttpResponse<String> response =
          HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("serverBaseUrl", normalized.getServerBaseUrl());
      result.put("syncHost", normalized.getSyncHost());
      result.put("syncPort", normalized.getSyncPort());
      result.put("httpStatus", response.statusCode());
      result.put("reachable", true);
      return result;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new BaseException("Remote server is not reachable: " + ex.getMessage(), 424);
    }
  }

  private static String resolveUrl(String url) {
    ClientConfig config = ClientConfigStore.requireConfigured();
    return joinUrl(config.getServerBaseUrl(), url);
  }

  private static String appendQuery(String url, Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return url;
    }

    StringJoiner query = new StringJoiner("&");
    params.forEach(
        (key, value) ->
            query.add(
                URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)));

    return url + (url.contains("?") ? "&" : "?") + query;
  }

  private static String joinUrl(String baseUrl, String url) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String path = url.startsWith("/") ? url : "/" + url;
    return base + path;
  }
}
