package backend.task;

import backend.mapper.sqlite.FileMapper;
import backend.mapper.sqlite.SubFileMapper;
import backend.model.entity.SubFile;
import backend.service.FileService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件变更监视任务。
 *
 * <p>每隔 30 秒扫描 DB 中所有 File 记录，检测磁盘内容与 DB 状态的差异：
 *
 * <p>1. 目录结构变更（lastModified 超过 update_time） → 调用 refreshSubFiles 将新增文件补录到 SubFile 表（is_sync =
 * false）
 *
 * <p>2. 已知 SubFile 内容变更（子文件 lastModified 超过父 File 的 update_time） → 将该 SubFile 标记为 is_sync = false
 *
 * <p>3. 单文件本身变更 → 将 File 标记为 is_sync = false
 *
 * <p>当任意变更被检测到时，父级 File 也会被标记为 is_sync = false。
 *
 * <p>该任务只负责"感知变化 → 打标记"，不触发实际同步。
 */
@Component
public class FileWatcherTask {

  private static final Logger logger = Logger.getLogger(FileWatcherTask.class.getName());

  private static final DateTimeFormatter SQLITE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Autowired private FileMapper fileMapper;

  @Autowired private SubFileMapper subFileMapper;

  @Autowired private FileService fileService;

  @Scheduled(fixedDelay = 30_000)
  public void watchFiles() {
    List<backend.model.entity.File> allFiles = fileMapper.selectAll();
    for (backend.model.entity.File file : allFiles) {
      if (file.getUpdateTime() == null || file.getPath() == null) continue;

      LocalDateTime dbTime = parseUpdateTime(file.getUpdateTime());
      if (dbTime == null) continue;

      if (Boolean.TRUE.equals(file.getIsDir())) {
        checkDirectory(file, dbTime);
      } else {
        checkSingleFile(file, dbTime);
      }
    }
  }

  // ── 单文件 ────────────────────────────────────────────────────────────────

  private void checkSingleFile(backend.model.entity.File file, LocalDateTime dbTime) {
    if (!Boolean.TRUE.equals(file.getIsSync())) return; // 已标记，跳过

    java.io.File local = new java.io.File(file.getPath());
    if (!local.exists()) return;

    if (isNewerOnDisk(local.lastModified(), dbTime)) {
      fileMapper.markAsStale(file.getId());
      logger.info("[FileWatcher] 文件已修改，标记待同步: " + file.getPath());
    }
  }

  // ── 目录 ─────────────────────────────────────────────────────────────────

  private void checkDirectory(backend.model.entity.File file, LocalDateTime dbTime) {
    java.io.File localRoot = new java.io.File(file.getPath());
    if (!localRoot.exists()) return;

    boolean anyChanged = false;

    // 步骤 1：目录本身的 lastModified 变化说明有文件被新增或删除
    //         → 将磁盘上存在但 SubFile 表里没有的条目补录进去
    if (isNewerOnDisk(localRoot.lastModified(), dbTime)) {
      fileService.refreshSubFiles(file.getId());
      anyChanged = true;
      logger.info("[FileWatcher] 目录结构变更，已补录新文件: " + file.getPath());
    }

    // 步骤 2：逐个检查已知 SubFile 的内容是否变更（不论目录是否变化都要检查）
    //         这样可以捕获 .zip 等被就地修改但目录结构未变的情况
    List<SubFile> subFiles = subFileMapper.selectByFileId(file.getId());
    for (SubFile sf : subFiles) {
      if (Boolean.TRUE.equals(sf.getIsDir())) continue;
      if (!Boolean.TRUE.equals(sf.getIsSync())) continue; // 已标记，跳过

      java.io.File localSub = new java.io.File(file.getPath(), sf.getRelativePath());
      if (!localSub.exists()) continue;

      if (isNewerOnDisk(localSub.lastModified(), dbTime)) {
        subFileMapper.markAsStale(sf.getId());
        anyChanged = true;
        logger.info("[FileWatcher] 子文件已修改，标记待同步: " + sf.getRelativePath());
      }
    }

    // 步骤 3：只要有任何变更，父级 File 也标记为待同步
    if (anyChanged && Boolean.TRUE.equals(file.getIsSync())) {
      fileMapper.markAsStale(file.getId());
    }
  }

  // ── 工具方法 ──────────────────────────────────────────────────────────────

  /** 将磁盘 lastModified（epoch ms）转换后与 dbTime 比较。 */
  private boolean isNewerOnDisk(long lastModifiedMs, LocalDateTime dbTime) {
    LocalDateTime diskTime =
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastModifiedMs), ZoneId.systemDefault());
    return diskTime.isAfter(dbTime);
  }

  /**
   * 兼容两种 update_time 格式： ISO-8601 (Java): "2024-01-15T10:30:45.123" SQLite datetime: "2024-01-15
   * 10:30:45"
   */
  private LocalDateTime parseUpdateTime(String s) {
    try {
      return LocalDateTime.parse(s);
    } catch (Exception ignored) {
    }
    try {
      return LocalDateTime.parse(s, SQLITE_FMT);
    } catch (Exception ignored) {
    }
    return null;
  }
}
