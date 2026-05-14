package backend.service.impl;

import backend.exception.model.BaseException;
import backend.mapper.sqlite.FileMapper;
import backend.mapper.sqlite.SubFileMapper;
import backend.mapper.sqlite.UserMapper;
import backend.model.entity.SubFile;
import backend.model.entity.User;
import backend.service.FileService;
import backend.util.HttpJsonClient;
import backend.util.SyncStyle;
import com.fasterxml.jackson.core.type.TypeReference;
import dataSync.CDCManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FileServiceImpl implements FileService {

  private static final Logger logger = Logger.getLogger(FileServiceImpl.class.getName());
  private static final int CONFLICT = 409;
  private static final String TASK_ALIAS_REQUIRED_MESSAGE =
      "\u4efb\u52a1\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a";
  private static final String TASK_PATH_REQUIRED_MESSAGE =
      "\u8bf7\u9009\u62e9\u672c\u5730\u8def\u5f84";
  private static final String DUPLICATE_TASK_ALIAS_MESSAGE =
      "\u4efb\u52a1\u540d\u79f0\u5df2\u5b58\u5728\uff0c\u8bf7\u66f4\u6362\u4e00\u4e2a\u540d\u79f0";
  private static final String TASK_NOT_FOUND_MESSAGE = "\u540c\u6b65\u4efb\u52a1\u4e0d\u5b58\u5728";

  @Autowired public UserMapper userMapper;

  @Autowired public FileMapper fileMapper;

  @Autowired public SubFileMapper subFileMapper;

  @Autowired
  @Qualifier("sqliteTransactionManager")
  private DataSourceTransactionManager sqliteTransactionManager;

  @Override
  public boolean upload(String path, String email) {
    return upload(0, path, email);
  }

  @Override
  public boolean upload(int fileId, String path, String email) {

    User u = requireLocalUser(email);

    backend.model.entity.File file = findUserTask(fileId, path, u.getId());

    if (file == null) return false;

    boolean success = sync(file, email);

    if (success) {
      fileMapper.updateSyncById(file.getId(), true);
      subFileMapper.updateSyncByFileId(file.getId(), true);
    }

    return success;
  }

  @Override
  public List<backend.model.entity.File> getBriefFileList(String email) {
    return fileMapper.selectById(requireLocalUser(email).getId());
  }

  private backend.model.entity.File findUserTask(int fileId, String path, int userId) {
    if (fileId > 0) {
      backend.model.entity.File task = fileMapper.selectByFileId(fileId);
      if (task != null && Objects.equals(task.getUserId(), userId)) {
        return task;
      }
      return null;
    }
    return fileMapper.selectByPath(path, userId);
  }

  @Override
  @Transactional(transactionManager = "sqliteTransactionManager")
  public backend.model.entity.File updateFileTask(
      int fileId,
      String alias,
      String path,
      String email,
      String scheduled,
      String alg,
      String desc,
      String remoteHost,
      boolean isDir) {

    String normalizedAlias = alias == null ? "" : alias.trim();
    String normalizedPath = path == null ? "" : path.trim();

    if (normalizedAlias.isEmpty()) throw new BaseException(TASK_ALIAS_REQUIRED_MESSAGE, CONFLICT);

    if (normalizedPath.isEmpty()) throw new BaseException(TASK_PATH_REQUIRED_MESSAGE, CONFLICT);

    User u = requireLocalUser(email);

    backend.model.entity.File originalTask = fileId > 0 ? fileMapper.selectByFileId(fileId) : null;

    if (fileId > 0
        && (originalTask == null || !Objects.equals(originalTask.getUserId(), u.getId()))) {
      throw new BaseException(TASK_NOT_FOUND_MESSAGE, CONFLICT);
    }

    Integer excludeId = originalTask == null ? null : originalTask.getId();
    if (fileMapper.countByAliasForUser(normalizedAlias, u.getId(), excludeId) > 0) {
      throw new BaseException(DUPLICATE_TASK_ALIAS_MESSAGE, CONFLICT);
    }

    backend.model.entity.File file =
        backend.model.entity.File.builder()
            .alias(normalizedAlias)
            .path(normalizedPath)
            .remoteHost(remoteHost)
            .scheduled(scheduled)
            .cdcAlg(alg)
            .isDir(isDir)
            .isSync(false)
            .description(desc)
            .userId(u.getId())
            .updateTime(LocalDateTime.now().toString())
            .build();

    // 在同一事务线程内执行，确保事务上下文生效
    addRoot(originalTask, file);
    Integer savedFileId = originalTask != null ? originalTask.getId() : file.getId();

    if (originalTask == null || !Objects.equals(originalTask.getPath(), normalizedPath)) {
      // 事务提交后再异步扫描，确保 addFiles 能读到已提交的 File 记录
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              CompletableFuture.runAsync(() -> addFiles(savedFileId, normalizedPath, email));
            }
          });
    }

    return savedFileId == null ? file : fileMapper.selectByFileId(savedFileId);
  }

  public boolean addRoot(backend.model.entity.File originalTask, backend.model.entity.File file) {
    if (originalTask != null) {
      file.setId(originalTask.getId());
      fileMapper.update(file);
    } else fileMapper.insert(file);

    return true;
  }

  @Override
  @Transactional(transactionManager = "sqliteTransactionManager")
  public Boolean deleteFileTask(String path, String email) {
    return deleteFileTask(0, path, email);
  }

  @Override
  @Transactional(transactionManager = "sqliteTransactionManager")
  public Boolean deleteFileTask(int fileId, String path, String email) {
    User u = requireLocalUser(email);
    backend.model.entity.File file = findUserTask(fileId, path, u.getId());
    if (file == null) return false;

    // 检查此 scope 是否还被某个有成员的群组共享
    String scopeName = buildScopeName(email, file);
    Boolean deletable =
        HttpJsonClient.postForData(
            "server/group/check-scope",
            Map.of("email", email, "scopeName", scopeName),
            new TypeReference<Boolean>() {});
    if (Boolean.FALSE.equals(deletable)) {
      throw new BaseException("无法删除：该文件夹已共享至群组，请先删除相关群组后再删除此任务", 409);
    }

    subFileMapper.deleteByFileId(file.getId());
    fileMapper.deleteById(file.getId());

    return true;
  }

  @Override
  public List<SubFile> getDetailedFileList(int fileId) {
    return subFileMapper.selectByFileIdAndParentIsNon(fileId);
  }

  @Override
  public boolean download(String path, String email) {
    return download(0, path, email);
  }

  @Override
  public boolean download(int fileId, String path, String email) {

    User u = requireLocalUser(email);

    backend.model.entity.File file = findUserTask(fileId, path, u.getId());

    if (file == null) {
      logger.warning("下行同步：本地未找到 File 记录，path=" + path);
      return false;
    }

    // 用 email/folderName 作为服务端同步范围名称（与上行同步保持一致）
    String scopeName = buildScopeName(email, file);

    // ── 第一步：获取服务端文件路径列表（不含内容） ──
    List<String> fileList =
        HttpJsonClient.postForData(
            "server/file/download",
            Map.of("scopeName", scopeName),
            new TypeReference<List<String>>() {});

    if (fileList == null || fileList.isEmpty()) {
      fileMapper.updateSyncById(file.getId(), true);
      return true;
    }

    File singleFileTarget = Boolean.TRUE.equals(file.getIsDir()) ? null : new File(file.getPath());
    if (!writeDownloadedFiles(scopeName, fileList, new File(file.getPath()), singleFileTarget)) {
      return false;
    }

    fileMapper.updateSyncById(file.getId(), true);
    subFileMapper.updateSyncByFileId(file.getId(), true);

    return true;
  }

  @Override
  public List<SubFile> getDetailedFileListByParent(int fileId) {

    List<SubFile> subFiles = subFileMapper.selectByParent(fileId);

    return subFiles;
  }

  /** 增量扫描目录，将磁盘上存在但 SubFile 表中缺失的条目补录进去（is_sync = false）。 不删除、不修改已有记录，仅做插入。 */
  @Override
  public void refreshSubFiles(int fileId) {
    backend.model.entity.File rootFile = fileMapper.selectByFileId(fileId);
    if (rootFile == null || !Boolean.TRUE.equals(rootFile.getIsDir())) return;

    File root = new File(rootFile.getPath());
    if (!root.exists()) return;

    List<SubFile> diskFiles = scanWithQueue(root, fileId);

    TransactionTemplate txTemplate = new TransactionTemplate(sqliteTransactionManager);
    txTemplate.execute(
        status -> {
          for (SubFile sf : diskFiles) {
            SubFile exist = subFileMapper.selectByFileIdAndPath(fileId, sf.getRelativePath());
            if (exist == null) {
              subFileMapper.insert(sf);
              logger.warning("新文件已补录至 SubFile 表: " + sf.getRelativePath());
            }
          }
          return null;
        });
  }

  @Override
  public boolean deleteFile(String email, String path) {
    SubFile target = subFileMapper.selectByPath(path);

    System.out.println(target);

    Queue<SubFile> deletedQueue = new LinkedList<>();
    deletedQueue.add(target);

    while (!deletedQueue.isEmpty()) {

      SubFile file = deletedQueue.poll();

      deletedQueue.addAll(subFileMapper.selectByParent(file.getId()));

      subFileMapper.deleteById(file.getId());
    }

    return true;
  }

  @Override
  public boolean downloadScope(String scopeName, String localPath) {
    return downloadScope(scopeName, localPath, null);
  }

  @Override
  public boolean downloadScope(String scopeName, String localPath, String relativePath) {
    List<String> fileList =
        HttpJsonClient.postForData(
            "server/file/download",
            Map.of("scopeName", scopeName),
            new TypeReference<List<String>>() {});

    if (fileList == null || fileList.isEmpty()) return true;

    return writeDownloadedFiles(
        scopeName, filterDownloadList(fileList, relativePath), new File(localPath), null);
  }

  private List<String> filterDownloadList(List<String> fileList, String requestedRelativePath) {
    String requested = normalizeRelativePath(requestedRelativePath);
    if (requested.isEmpty()) return fileList;

    if (fileList.contains(requested)) {
      return List.of(requested);
    }

    String prefix = requested.endsWith("/") ? requested : requested + "/";
    return fileList.stream().filter(path -> path.startsWith(prefix)).toList();
  }

  private boolean writeDownloadedFiles(
      String scopeName, List<String> fileList, File rootDir, File singleFileTarget) {
    if (fileList == null || fileList.isEmpty()) return true;

    if (singleFileTarget == null) {
      rootDir.mkdirs();
    } else if (singleFileTarget.getParentFile() != null) {
      singleFileTarget.getParentFile().mkdirs();
    }

    for (String relativePath : fileList) {
      File target = singleFileTarget == null ? new File(rootDir, relativePath) : singleFileTarget;
      if (target.getParentFile() != null) target.getParentFile().mkdirs();

      byte[] bytes;
      try {
        bytes =
            HttpJsonClient.downloadBytes(
                "server/file/download/file",
                Map.of("scopeName", scopeName, "relativePath", relativePath));
      } catch (RuntimeException e) {
        logger.warning("下载文件失败，跳过: " + relativePath + " - " + e.getMessage());
        return false;
      }

      try (FileOutputStream fos = new FileOutputStream(target)) {
        fos.write(bytes);
      } catch (IOException e) {
        logger.warning("写入文件失败: " + target.getAbsolutePath() + " - " + e.getMessage());
        return false;
      }
    }
    return true;
  }

  public boolean sync(backend.model.entity.File task, String email) {

    String path = task.getPath();
    String className = task.getCdcAlg();
    File rootFile = new File(path);
    if (!rootFile.exists()) return false;

    List<SyncStyle> fileList = new LinkedList<>();

    CDCManager cdcManager;
    try {
      Map<String, String> algClassMap =
          Map.of(
              "FastCDC", "dataSync.FastCDC.FastCDCManager",
              "RabinCDC", "dataSync.RabinCDC.RabinCDCManager",
              "FlipCDC", "dataSync.FlipCDC.FlipCDCManager",
              "QuickCDC", "dataSync.QuickCDC.QuickCDCManager");
      String fullClassName = algClassMap.getOrDefault(className, className);
      Class<?> clazz = Class.forName(fullClassName);
      cdcManager = (CDCManager) clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      logger.warning("无法加载 CDC 算法类: " + className + " - " + e.getMessage());
      return false;
    }

    File baseDir = rootFile.getParentFile();
    if (baseDir == null) {
      baseDir = rootFile;
    }

    processFiles(baseDir, rootFile, cdcManager, fileList);

    String storageRoot = joinRemotePath(email, task.getAlias());
    String scopeName = buildScopeName(email, task);
    for (SyncStyle s : fileList) {
      String normalized = s.storagePath != null ? s.storagePath.replace("\\", "/") : "";
      s.storagePath = normalized.isEmpty() ? storageRoot : joinRemotePath(storageRoot, normalized);
    }

    HttpJsonClient.postForData(
        "server/file/compare",
        Map.of(
            "list", fileList,
            "path", path,
            "email", email,
            "scopeName", scopeName,
            "isDir", Boolean.TRUE.equals(task.getIsDir())),
        new TypeReference<List<SyncStyle>>() {});

    if (fileList.isEmpty()) return true;

    return uploadFilesOverHttp(fileList);
  }

  private boolean uploadFilesOverHttp(List<SyncStyle> fileList) {
    for (SyncStyle style : fileList) {
      if (style == null || style.file == null || !style.file.exists() || !style.file.isFile()) {
        continue;
      }

      try {
        HttpJsonClient.uploadFile(
            "server/file/upload",
            style.file,
            Map.of(
                "storagePath",
                normalizeStoragePath(style.storagePath),
                "fileName",
                style.file.getName()));
      } catch (RuntimeException e) {
        logger.warning(
            "HTTP upload failed: " + style.file.getAbsolutePath() + " - " + e.getMessage());
        return false;
      }
    }
    return true;
  }

  private String buildScopeName(String email, backend.model.entity.File task) {
    return joinRemotePath(email, task.getAlias(), getRootName(task.getPath()));
  }

  private String getRootName(String path) {
    return new File(path).getName();
  }

  private String joinRemotePath(String... parts) {
    List<String> normalizedParts = new ArrayList<>();
    for (String part : parts) {
      String normalized = normalizeRelativePath(part);
      if (!normalized.isBlank()) {
        normalizedParts.add(normalized);
      }
    }
    return String.join("/", normalizedParts);
  }

  private String normalizeRelativePath(String path) {
    String normalized = path == null ? "" : path.replace("\\", "/").trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String normalizeStoragePath(String storagePath) {
    String normalized = storagePath == null ? "" : storagePath.replace("\\", "/").trim();
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (normalized.isBlank()) {
      throw new BaseException("Remote storage path is empty.", 400);
    }
    for (String segment : normalized.split("/")) {
      if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
        throw new BaseException("Remote storage path is invalid.", 400);
      }
    }
    return normalized;
  }

  public Boolean addFiles(String path, String email) {
    return addFiles(null, path, email);
  }

  public Boolean addFiles(Integer fileId, String path, String email) {
    File root = new File(path);
    if (!root.exists()) {
      System.err.println("错误：根路径不存在 " + path);
      return false;
    }

    User u = requireLocalUser(email);

    // 使用 TransactionTemplate 确保同一事务内提交，避免 auto-commit=false 导致写入不生效
    TransactionTemplate txTemplate = new TransactionTemplate(sqliteTransactionManager);
    try {
      return Boolean.TRUE.equals(
          txTemplate.execute(
              status -> {
                backend.model.entity.File rootFileRecord =
                    upsertRootFileRecordInTransaction(fileId, path, root, u.getId());
                if (rootFileRecord == null || rootFileRecord.getId() == null) {
                  System.err.println("错误：创建或更新主文件记录失败");
                  status.setRollbackOnly();
                  return false;
                }
                List<SubFile> subFileList = scanWithQueue(root, rootFileRecord.getId());
                saveSubFileListInTransaction(rootFileRecord.getId(), subFileList);
                return true;
              }));
    } catch (Exception e) {
      System.err.println("处理文件列表时发生异常: " + e.getMessage());
      return false;
    }
  }

  /** BFS 队列扫描，天然保证父目录先于子节点入列。 队列元素携带构建 SubFile 所需的全部上下文，不依赖递归调用栈。 */
  private List<SubFile> scanWithQueue(File root, Integer fileId) {
    // 队列元素：待处理的文件、其相对路径、其深度、其父相对路径
    record QueueItem(File file, String relativePath, int depth, String parentRelativePath) {}

    List<SubFile> result = new ArrayList<>();
    Queue<QueueItem> queue = new LinkedList<>();

    if (root.isDirectory()) {
      // 目录任务：root 本身已在 File 表中，只把其直接子节点入队
      File[] children = root.listFiles();
      if (children != null) {
        for (File child : children) {
          queue.offer(new QueueItem(child, child.getName(), 0, ""));
        }
      }
    } else if (root.isFile()) {
      // 单文件任务：File 表里的 root 自身即任务文件；为其在 SubFile 表登记一条记录，
      // 这样列表 UI 能像目录任务一样列出该条目，下行/删除等流程亦可统一处理。
      queue.offer(new QueueItem(root, root.getName(), 0, ""));
    }

    while (!queue.isEmpty()) {
      QueueItem item = queue.poll();
      File current = item.file();

      SubFile subFile =
          SubFile.builder()
              .fileId(fileId)
              .name(current.getName())
              .relativePath(item.relativePath())
              .isDir(current.isDirectory())
              .isSync(false)
              .depth(item.depth())
              .build();

      result.add(subFile);

      // 若是目录，将其子节点继续入队
      if (current.isDirectory()) {
        File[] children = current.listFiles();
        if (children != null) {
          for (File child : children) {
            String childRelativePath = item.relativePath() + "/" + child.getName();
            queue.offer(
                new QueueItem(child, childRelativePath, item.depth() + 1, item.relativePath()));
          }
        }
      }
    }

    return result;
  }

  @Transactional(transactionManager = "sqliteTransactionManager")
  public void saveSubFileListInTransaction(Integer fileId, List<SubFile> subFiles) {
    subFileMapper.deleteByFileId(fileId);
    if (subFiles == null || subFiles.isEmpty()) return;

    // BFS 已保证父先子后，无需额外排序
    Map<String, Integer> pathToIdMap = new HashMap<>();

    for (SubFile subFile : subFiles) {
      String relativePath = subFile.getRelativePath();
      int lastSep = relativePath.lastIndexOf('/');
      String parentPath = (lastSep == -1) ? "" : relativePath.substring(0, lastSep);

      // "" 对应根级节点，parentId = null
      Integer parentId = pathToIdMap.get(parentPath);
      subFile.setParent(parentId);

      SubFile exist = subFileMapper.selectByFileIdAndPath(fileId, relativePath);

      if (exist == null) {
        subFileMapper.insert(subFile);
      } else {
        subFileMapper.update(subFile);
      }

      if (Boolean.TRUE.equals(subFile.getIsDir())) {
        pathToIdMap.put(relativePath, subFile.getId());
      }
    }
  }

  @Transactional(transactionManager = "sqliteTransactionManager")
  public backend.model.entity.File upsertRootFileRecordInTransaction(
      Integer fileId, String path, File root, Integer userId) {
    boolean targetById = fileId != null && fileId > 0;
    backend.model.entity.File rootFileRecord = null;

    if (targetById) {
      rootFileRecord = fileMapper.selectByFileId(fileId);
      if (rootFileRecord != null && !Objects.equals(rootFileRecord.getUserId(), userId)) {
        return null;
      }
    }

    if (rootFileRecord == null) {
      rootFileRecord = fileMapper.selectByPath(path, userId);
      targetById = false;
    }

    if (rootFileRecord == null) {
      rootFileRecord =
          backend.model.entity.File.builder()
              .path(path)
              .alias(root.getName())
              .isDir(root.isDirectory())
              .isSync(false)
              .userId(userId)
              .updateTime(LocalDateTime.now().toString())
              .build();
      fileMapper.insert(rootFileRecord);
    } else {
      if (!targetById) {
        rootFileRecord.setAlias(root.getName());
      }
      rootFileRecord.setPath(path);
      rootFileRecord.setIsDir(root.isDirectory());
      rootFileRecord.setUpdateTime(LocalDateTime.now().toString());
      fileMapper.update(rootFileRecord);
    }
    return rootFileRecord;
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

  private void processFiles(
      File baseDir, File currentFile, CDCManager cdcManager, List<SyncStyle> list) {
    if (currentFile.isDirectory()) {
      File[] files = currentFile.listFiles();
      if (files != null) {
        for (File f : files) {
          processFiles(baseDir, f, cdcManager, list);
        }
      }
    } else {
      String relativePath = getRelativePath(baseDir, currentFile);

      list.add(new SyncStyle(currentFile, cdcManager, relativePath));
    }
  }

  private User requireLocalUser(String email) {
    String normalizedEmail = email == null ? "" : email.trim();
    if (normalizedEmail.isEmpty()) {
      throw new BaseException("用户邮箱不能为空", 400);
    }

    User user = userMapper.selectByEmail(normalizedEmail);
    if (user != null && user.getId() != null) {
      return user;
    }

    Map<String, Object> remoteUser =
        HttpJsonClient.postForData(
            "server/user/resolve", Map.of("email", normalizedEmail), new TypeReference<>() {});

    User restoredUser =
        User.builder()
            .id(parseUserId(remoteUser.get("id")))
            .username(asString(remoteUser.get("username")))
            .email(asString(remoteUser.get("email")))
            .avatar(asString(remoteUser.get("avatar")))
            .build();

    if (restoredUser.getId() == null || restoredUser.getEmail() == null) {
      throw new BaseException("无法恢复本地用户缓存，请重新登录。", 428);
    }

    userMapper.insert(restoredUser);
    return restoredUser;
  }

  private Integer parseUserId(Object value) {
    if (value == null) return null;
    if (value instanceof Number number) return number.intValue();
    String text = value.toString().trim();
    return text.isEmpty() ? null : Integer.valueOf(text);
  }

  private String asString(Object value) {
    if (value == null) return null;
    String text = value.toString();
    return text.isBlank() ? null : text;
  }
}
