package backend.service.impl;

import backend.service.FileService;
import backend.util.SyncStyle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
      String email,
      String clientRawPath,
      String scopeName,
      boolean isDir,
      List<SyncStyle> clientFileList) {

    // 1. 获取服务端根目录
    File serverBase = serverBaseDir();

    File syncTarget = resolveStoragePath(scopeName);
    if (isDir) {
      syncTarget.mkdirs();
    } else if (syncTarget.getParentFile() != null) {
      syncTarget.getParentFile().mkdirs();
    }
    File taskContainer =
        syncTarget.getParentFile() == null ? syncTarget : syncTarget.getParentFile();

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
    if (taskContainer.isFile()) {
      if (!taskContainer.getName().endsWith(".part")) {
        existingServerFiles.add(taskContainer);
      }
    } else if (taskContainer.isDirectory()) {
      scanServerFiles(taskContainer, existingServerFiles);
    }

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
    if (taskContainer.isDirectory()) {
      cleanEmptyDirectories(taskContainer);
    }

    return clientFileList;
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

  private File serverBaseDir() {
    File serverBase = new File(basePath);
    if (!serverBase.exists()) {
      serverBase.mkdirs();
    }
    return serverBase;
  }

  private File resolveStoragePath(String storagePath) {
    File serverBase = serverBaseDir();
    File target = new File(serverBase, normalizeStoragePath(storagePath));

    try {
      String canonicalBase = serverBase.getCanonicalPath();
      String canonicalTarget = target.getCanonicalPath();
      if (!isInside(canonicalBase, canonicalTarget)) {
        throw new SecurityException("Illegal storagePath: " + storagePath);
      }
      return target;
    } catch (IOException e) {
      throw new RuntimeException("路径解析失败: " + e.getMessage(), e);
    }
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

    File scopeDir = resolveStoragePath(scopeName);

    if (!scopeDir.exists()) {
      logger.info("下行同步：范围不存在 " + scopeDir.getAbsolutePath());
      return result;
    }

    if (scopeDir.isFile()) {
      result.add(scopeDir.getName());
      return result;
    }

    if (!scopeDir.isDirectory()) return result;

    collectRelativePaths(scopeDir, scopeDir, result);
    return result;
  }

  /** 下行同步第二步：读取单个文件的原始字节，直接返回给客户端。 */
  @Override
  public byte[] downloadFile(String scopeName, String relativePath) {
    if (scopeName == null || scopeName.isBlank()) throw new SecurityException("scopeName 不能为空");
    if (relativePath == null || relativePath.isBlank())
      throw new SecurityException("relativePath 不能为空");

    File scopeDir = resolveStoragePath(scopeName);

    if (scopeDir.isFile()) {
      if (!scopeDir.getName().equals(relativePath)) {
        throw new SecurityException("非法路径: " + relativePath);
      }
      try {
        return Files.readAllBytes(scopeDir.toPath());
      } catch (IOException e) {
        throw new RuntimeException(
            "读取文件失败: " + scopeDir.getAbsolutePath() + " - " + e.getMessage(), e);
      }
    }

    File target = new File(scopeDir, relativePath);

    // 安全校验：防止路径穿越
    try {
      String canonicalScope = scopeDir.getCanonicalPath();
      String canonicalTarget = target.getCanonicalPath();
      if (!isInside(canonicalScope, canonicalTarget)) {
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

  @Override
  public long uploadFile(String storagePath, String fileName, InputStream inputStream) {
    String safeStoragePath = normalizeStoragePath(storagePath);
    if (fileName == null || fileName.isBlank()) {
      throw new SecurityException("fileName is required");
    }
    if (fileName.contains("/")
        || fileName.contains("\\")
        || ".".equals(fileName)
        || "..".equals(fileName)) {
      throw new SecurityException("Illegal fileName: " + fileName);
    }

    File serverBase = new File(basePath);
    File targetDir = new File(serverBase, safeStoragePath);
    File target = new File(targetDir, fileName);
    File temp = new File(targetDir, fileName + ".part");

    try {
      String canonicalBase = serverBase.getCanonicalPath();
      String canonicalTargetDir = targetDir.getCanonicalPath();
      String canonicalTarget = target.getCanonicalPath();

      if (!isInside(canonicalBase, canonicalTargetDir)
          || !isInside(canonicalTargetDir, canonicalTarget)) {
        throw new SecurityException("Illegal upload path: " + safeStoragePath + "/" + fileName);
      }

      Files.createDirectories(targetDir.toPath());
      long bytes;
      try (inputStream) {
        bytes = Files.copy(inputStream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      moveUploadedFile(temp, target);
      return bytes;
    } catch (IOException e) {
      throw new RuntimeException(
          "Upload file failed: " + target.getAbsolutePath() + " - " + e.getMessage(), e);
    }
  }

  private String normalizeStoragePath(String storagePath) {
    if (storagePath == null || storagePath.isBlank()) {
      throw new SecurityException("storagePath is required");
    }

    String normalized = storagePath.replace("\\", "/").trim();
    if (normalized.startsWith("/") || normalized.contains("\u0000")) {
      throw new SecurityException("Illegal storagePath: " + storagePath);
    }

    for (String segment : normalized.split("/")) {
      if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
        throw new SecurityException("Illegal storagePath: " + storagePath);
      }
    }
    return normalized;
  }

  private boolean isInside(String canonicalBase, String canonicalTarget) {
    return canonicalTarget.equals(canonicalBase)
        || canonicalTarget.startsWith(canonicalBase + File.separator);
  }

  private void moveUploadedFile(File temp, File target) throws IOException {
    try {
      Files.move(
          temp.toPath(),
          target.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException atomicMoveFailed) {
      Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
