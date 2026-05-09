package backend.service.impl;

import backend.service.FileService;
import backend.util.SyncStyle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

  @Value("${spring.netty.server.basePath}")
  public String basePath;

  private final Logger logger = Logger.getLogger(FileServiceImpl.class.getName());

  @Override
  public List<SyncStyle> compare(
      String email, String clientRawPath, List<SyncStyle> clientFileList) {

    // 1. 获取服务端根目录
    File serverBase = new File(basePath);
    if (!serverBase.exists()) {
      serverBase.mkdirs();
    }

    String syncScopeName = email + "/" + getFileNameFromPath(clientRawPath);

    File syncTargetDir = new File(serverBase, syncScopeName);
    if (!syncTargetDir.exists()) {
      syncTargetDir.mkdirs();
    }

    // 2. 构建客户端文件索引
    Set<String> clientFileIndex = new HashSet<>();
    if (clientFileList != null) {
      for (SyncStyle style : clientFileList) {
        if (style.file != null) {
          String key = generateKey(style.storagePath, style.file.getName());
          clientFileIndex.add(key);
        }
      }
    }

    // 3. 递归扫描服务端
    List<File> existingServerFiles = new ArrayList<>();
    scanServerFiles(syncTargetDir, existingServerFiles);

    // 4. 执行反向对比与删除
    for (File serverFile : existingServerFiles) {
      String serverRelativeDir = getRelativePath(serverBase, serverFile);
      String serverFileName = serverFile.getName();
      String serverKey = generateKey(serverRelativeDir, serverFileName);

      if (!clientFileIndex.contains(serverKey)) {
        boolean deleted = serverFile.delete();
        if (!deleted) {
          logger.info("无法删除文件: " + serverFile.getAbsolutePath());
        }
      }
    }

    // 5. 清理空目录 -> 修复点 C：只清理本次同步的目标文件夹
    cleanEmptyDirectories(syncTargetDir);

    return clientFileList;
  }

  /** 跨平台从路径字符串中提取文件名/最后一级目录名 */
  private String getFileNameFromPath(String path) {
    if (path == null || path.isEmpty()) return "unknown";

    String normalized = path.replace("\\", "/");

    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    int lastSlash = normalized.lastIndexOf("/");
    if (lastSlash >= 0) {
      return normalized.substring(lastSlash + 1);
    }
    return normalized;
  }

  /** 生成标准化的文件唯一标识 Key */
  private String generateKey(String relativeDir, String fileName) {
    if (relativeDir == null || relativeDir.isEmpty()) {
      return fileName;
    }

    String normalizedDir = relativeDir.replace("\\", "/");

    if (normalizedDir.startsWith("/")) normalizedDir = normalizedDir.substring(1);
    if (normalizedDir.endsWith("/"))
      normalizedDir = normalizedDir.substring(0, normalizedDir.length() - 1);

    return normalizedDir + "/" + fileName;
  }

  /** 递归获取服务端目录下所有文件 */
  private void scanServerFiles(File dir, List<File> result) {
    File[] files = dir.listFiles();
    if (files == null) return;

    for (File f : files) {
      if (f.isDirectory()) {
        scanServerFiles(f, result);
      } else {
        if (!f.getName().endsWith(".part")) {
          result.add(f);
        }
      }
    }
  }

  /** 递归删除空目录 */
  private void cleanEmptyDirectories(File dir) {
    if (dir != null && dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File f : files) {
          if (f.isDirectory()) {
            cleanEmptyDirectories(f);
          }
        }
      }
      files = dir.listFiles();
      if (files != null
          && files.length == 0
          && !dir.getAbsolutePath().equals(new File(basePath).getAbsolutePath())) {
        dir.delete();
      }
    }
  }

  /** 获取文件相对路径 */
  private String getRelativePath(File baseDir, File file) {
    String basePath = baseDir.getAbsolutePath();
    String parentPath = file.getParentFile().getAbsolutePath();

    if (parentPath.equals(basePath)) {
      return "";
    }
    if (parentPath.startsWith(basePath)) {
      // +1 是为了去掉开头的分隔符
      return parentPath.substring(basePath.length() + 1);
    }
    return "";
  }

  /** 下行同步第一步：扫描 basePath/scopeName，返回所有文件的相对路径列表（不含内容）。 */
  @Override
  public List<String> listDownloadFiles(String scopeName) {
    if (scopeName == null || scopeName.isBlank()) return new ArrayList<>();

    List<String> result = new ArrayList<>();

    File serverBase = new File(basePath);
    File scopeDir = new File(serverBase, scopeName);

    try {
      String canonicalBase = serverBase.getCanonicalPath();
      String canonicalScope = scopeDir.getCanonicalPath();
      if (!canonicalScope.startsWith(canonicalBase + File.separator)
          && !canonicalScope.equals(canonicalBase)) {
        throw new SecurityException("非法 scopeName: " + scopeName);
      }
    } catch (IOException e) {
      throw new RuntimeException("路径解析失败: " + e.getMessage(), e);
    }

    if (!scopeDir.exists() || !scopeDir.isDirectory()) {
      logger.info("下行同步：范围目录不存在 " + scopeDir.getAbsolutePath());
      return result;
    }

    collectRelativePaths(scopeDir, scopeDir, result);
    return result;
  }

  /** 下行同步第二步：读取单个文件的原始字节，直接返回给客户端。 */
  @Override
  public byte[] downloadFile(String scopeName, String relativePath) {
    if (scopeName == null || scopeName.isBlank()) throw new SecurityException("scopeName 不能为空");
    if (relativePath == null || relativePath.isBlank())
      throw new SecurityException("relativePath 不能为空");

    File serverBase = new File(basePath);
    File scopeDir = new File(serverBase, scopeName);

    try {
      String canonicalBase = serverBase.getCanonicalPath();
      String canonicalScope = scopeDir.getCanonicalPath();
      if (!canonicalScope.startsWith(canonicalBase + File.separator)
          && !canonicalScope.equals(canonicalBase)) {
        throw new SecurityException("非法 scopeName: " + scopeName);
      }
    } catch (IOException e) {
      throw new RuntimeException("路径解析失败: " + e.getMessage(), e);
    }

    File target = new File(scopeDir, relativePath);

    // 安全校验：防止路径穿越
    try {
      String canonicalScope = scopeDir.getCanonicalPath();
      String canonicalTarget = target.getCanonicalPath();
      if (!canonicalTarget.startsWith(canonicalScope)) {
        throw new SecurityException("非法路径: " + relativePath);
      }
    } catch (IOException e) {
      throw new RuntimeException("路径解析失败: " + e.getMessage(), e);
    }

    if (!target.exists() || !target.isFile()) {
      logger.warning("下行同步：文件不存在 " + target.getAbsolutePath());
      return new byte[0];
    }

    try {
      return Files.readAllBytes(target.toPath());
    } catch (IOException e) {
      throw new RuntimeException("读取文件失败: " + target.getAbsolutePath() + " - " + e.getMessage(), e);
    }
  }

  /** 递归收集目录下所有文件的相对路径，跳过临时文件（.part）。 */
  private void collectRelativePaths(File scopeRoot, File current, List<String> result) {
    File[] children = current.listFiles();
    if (children == null) return;

    for (File child : children) {
      if (child.isDirectory()) {
        collectRelativePaths(scopeRoot, child, result);
      } else if (!child.getName().endsWith(".part")) {
        result.add(buildRelativePath(scopeRoot, child));
      }
    }
  }

  /** 构建文件相对于 scopeRoot 的路径（使用正斜杠，兼容跨平台）。 */
  private String buildRelativePath(File scopeRoot, File file) {
    String rootPath = scopeRoot.getAbsolutePath();
    String filePath = file.getAbsolutePath();

    if (filePath.startsWith(rootPath)) {
      String rel = filePath.substring(rootPath.length());
      // 去掉开头的分隔符并统一为正斜杠
      rel = rel.replaceAll("^[/\\\\]+", "").replace("\\", "/");
      return rel;
    }
    return file.getName();
  }
}
