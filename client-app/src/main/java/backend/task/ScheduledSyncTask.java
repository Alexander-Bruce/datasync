package backend.task;

import backend.mapper.sqlite.FileMapper;
import backend.mapper.sqlite.UserMapper;
import backend.model.entity.User;
import backend.service.FileService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时同步任务。
 *
 * <p>每分钟扫描所有 File 记录，根据 {@code scheduled} 字段决定是否触发上行同步。
 *
 * <p>scheduled 字段格式（大小写不敏感）： "5m" → 每 5 分钟 "30m" → 每 30 分钟 "1h" → 每 1 小时 "6h" → 每 6 小时 "1d" → 每 1 天
 * 空值 / "never" → 禁用
 *
 * <p>上次触发时间以内存 Map 跟踪；应用重启后所有任务将在第一个触发周期内执行一次。
 */
@Component
public class ScheduledSyncTask {

  private static final Logger logger = Logger.getLogger(ScheduledSyncTask.class.getName());

  /** fileId → 上次成功触发定时同步的时刻 */
  private final Map<Integer, Instant> lastTriggered = new ConcurrentHashMap<>();

  @Autowired private FileMapper fileMapper;

  @Autowired private UserMapper userMapper;

  @Autowired private FileService fileService;

  @Scheduled(fixedDelay = 60_000)
  public void runScheduledSyncs() {
    List<backend.model.entity.File> allFiles = fileMapper.selectAll();
    Instant now = Instant.now();

    for (backend.model.entity.File file : allFiles) {
      String scheduled = file.getScheduled();
      if (scheduled == null || scheduled.isBlank()) continue;

      Duration interval = parseInterval(scheduled);
      if (interval == null) continue;

      Instant last = lastTriggered.getOrDefault(file.getId(), Instant.EPOCH);
      if (Duration.between(last, now).compareTo(interval) >= 0) {
        triggerUpload(file);
        lastTriggered.put(file.getId(), now);
      }
    }
  }

  private void triggerUpload(backend.model.entity.File file) {
    User user = userMapper.selectById(file.getUserId());
    if (user == null) {
      logger.warning("[ScheduledSync] 找不到用户，跳过: fileId=" + file.getId());
      return;
    }
    try {
      boolean ok = fileService.upload(file.getId(), file.getPath(), user.getEmail());
      if (ok) {
        logger.info("[ScheduledSync] 定时上行同步完成: " + file.getPath());
      } else {
        logger.warning("[ScheduledSync] 定时上行同步返回 false: " + file.getPath());
      }
    } catch (Exception e) {
      logger.warning("[ScheduledSync] 定时上行同步异常: " + file.getPath() + " - " + e.getMessage());
    }
  }

  /**
   * 解析定时间隔字符串，返回 Duration；无法识别时返回 null。
   *
   * <p>支持格式：Xm（分钟）、Xh（小时）、Xd（天）、纯数字（视为分钟）
   */
  static Duration parseInterval(String s) {
    if (s == null) return null;
    String v = s.trim().toLowerCase();
    if (v.isEmpty() || v.equals("never")) return null;

    try {
      if (v.endsWith("d")) {
        long days = Long.parseLong(v.substring(0, v.length() - 1));
        return Duration.ofDays(days);
      }
      if (v.endsWith("h")) {
        long hours = Long.parseLong(v.substring(0, v.length() - 1));
        return Duration.ofHours(hours);
      }
      if (v.endsWith("m")) {
        long minutes = Long.parseLong(v.substring(0, v.length() - 1));
        return Duration.ofMinutes(minutes);
      }
      // 纯数字视为分钟
      return Duration.ofMinutes(Long.parseLong(v));
    } catch (NumberFormatException e) {
      logger.warning("[ScheduledSync] 无法解析 scheduled 值: \"" + s + "\"，已跳过");
      return null;
    }
  }
}
