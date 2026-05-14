package backend.migration;

import backend.model.Group;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 把旧 {@code email/rootName} 布局迁移到新 {@code email/alias/rootName} 布局，容器启动时执行一次。
 *
 * <p>HF Space 的 bucket 走 FUSE 挂载，不支持原子目录 rename；如果用 {@code Files.move(dir, …)}
 * 整个目录搬，常常会静默失败。这里改成「逐文件移动」：递归遍历旧目录里的每个普通文件，按相对路径搬到新目录，最后再清理原来留下的空目录。 这样无论底层是真文件系统还是对象存储桶，都能稳定推进。
 *
 * <p>历史 bucket 里的 alias 等于 rootName（旧客户端的默认行为），所以 {@code email/X → email/X/X} 是无损可逆的等价变换。私有任务（不在
 * groups.json 中）也会被遍历，让桶里所有遗留 scope 都能跟上新代码而不产生孤儿文件。
 */
@Component
@Profile("docker")
public class ScopeStorageMigrationRunner implements ApplicationRunner {

  private static final Logger logger =
      Logger.getLogger(ScopeStorageMigrationRunner.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String LEGACY_TEMP_PREFIX = ".__legacy__";

  @Value("${spring.netty.server.basePath}")
  private String basePath;

  @Override
  public void run(ApplicationArguments args) {
    try {
      MigrationReport report = migrateAll();
      logger.info(
          "Legacy scope migration done: scanned="
              + report.scanned
              + " migrated="
              + report.migrated
              + " skipped="
              + report.skipped
              + " failed="
              + report.failed
              + " groupScopes="
              + report.groupScopes);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Legacy scope migration failed: " + e.getMessage(), e);
    }
  }

  /** Public so an admin endpoint can re-trigger it after startup if needed. */
  public MigrationReport migrateAll() throws IOException {
    MigrationReport report = new MigrationReport();
    File baseDir = new File(basePath);
    if (!baseDir.exists() && !baseDir.mkdirs()) {
      logger.warning("Migration skipped: cannot create basePath " + basePath);
      return report;
    }

    migrateAllEmailDirectories(baseDir.toPath(), report);
    report.groupScopes = migrateGroupsFile(new File(baseDir, "groups.json"));
    return report;
  }

  /** 扫描 {@code basePath/<email>/*}，对每个尚未迁移的 scope 执行 {@code email/X → email/X/X} 的转换。 */
  private void migrateAllEmailDirectories(Path baseDir, MigrationReport report) throws IOException {
    try (DirectoryStream<Path> emails = Files.newDirectoryStream(baseDir)) {
      for (Path emailDir : emails) {
        String emailName = emailDir.getFileName().toString();
        if (!Files.isDirectory(emailDir)) continue;
        if (emailName.startsWith(LEGACY_TEMP_PREFIX)) continue;
        if (!looksLikeEmail(emailName)) continue;

        List<Path> children;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(emailDir)) {
          children = new ArrayList<>();
          entries.forEach(children::add);
        }
        children.sort(Comparator.comparing(p -> p.getFileName().toString()));

        for (Path entry : children) {
          String entryName = entry.getFileName().toString();
          if (entryName.startsWith(LEGACY_TEMP_PREFIX)) continue;
          if (isAlreadyMigrated(entry)) {
            report.skipped++;
            continue;
          }
          report.scanned++;

          Path target = entry.resolve(entryName);
          try {
            if (Files.isRegularFile(entry)) {
              migrateSingleFile(entry, target);
            } else if (Files.isDirectory(entry)) {
              migrateDirectoryContents(entry, target);
            } else {
              continue;
            }
            report.migrated++;
            logger.info(
                "Migrated legacy scope: "
                    + emailName
                    + "/"
                    + entryName
                    + " -> "
                    + emailName
                    + "/"
                    + entryName
                    + "/"
                    + entryName);
          } catch (IOException e) {
            report.failed++;
            logger.warning(
                "Failed migrating " + emailName + "/" + entryName + ": " + e.getMessage());
          }
        }
      }
    }
  }

  /**
   * 单文件 scope：file → temp → mkdir(原路径) → temp → 原路径/file。 用临时位置中转，是为了让原始 file 路径腾出来给同名 alias 目录使用。
   */
  private void migrateSingleFile(Path legacyFile, Path target) throws IOException {
    Path parent = legacyFile.getParent();
    if (parent == null) {
      throw new IOException("Legacy file has no parent: " + legacyFile);
    }
    Path temp = uniqueTempPath(parent, legacyFile.getFileName().toString());
    moveSafely(legacyFile, temp);
    Files.createDirectories(target.getParent());
    moveSafely(temp, target);
  }

  /**
   * 目录 scope：遍历 legacyDir 下的每个普通文件，逐个搬到 target/相对路径。然后再清理 legacyDir 内残留的空目录。 整个过程不依赖目录原子
   * rename，FUSE 挂载点也能稳。
   *
   * <p>必须先把目录树物理化（toAbsolutePath().normalize()），避免遍历过程中相对路径解析依赖运行时 cwd。
   */
  private void migrateDirectoryContents(Path legacyDir, Path target) throws IOException {
    Path normalizedLegacy = legacyDir.toAbsolutePath().normalize();
    Path normalizedTarget = target.toAbsolutePath().normalize();

    // 第一遍：把所有 regular file 搬到新位置。遍历时跳过 target 子树本身（它会随着第一次写入被创建），避免重复处理。
    Files.walkFileTree(
        normalizedLegacy,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            if (dir.equals(normalizedTarget)) {
              // 第一次移动文件时会创建 target 目录，遍历到它时直接跳过，避免递归把刚搬过去的文件再搬一遍。
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Path rel = normalizedLegacy.relativize(file);
            Path dest = normalizedTarget.resolve(rel);
            Files.createDirectories(dest.getParent());
            moveSafely(file, dest);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            // 单个文件失败不让整次迁移崩，只是记一笔。整体成败由调用方根据 emptyDir 残留情况判断。
            logger.warning("Skipping unreadable file during migration: " + file + " - " + exc);
            return FileVisitResult.CONTINUE;
          }
        });

    // 第二遍：清理 legacyDir 中残留的空目录（保留 target 本身，因为它现在装着所有搬过来的文件）。
    deleteEmptyDirectoriesExcept(normalizedLegacy, normalizedTarget);
  }

  /** 从最深处往外删除空目录；遇到 target 子树直接跳过；遇到非空目录就停在那一层。 */
  private void deleteEmptyDirectoriesExcept(Path root, Path keep) throws IOException {
    if (!Files.isDirectory(root)) return;
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.equals(keep)) return FileVisitResult.SKIP_SUBTREE;
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (dir.equals(root)) return FileVisitResult.CONTINUE; // 保留入口目录，由调用方决定要不要删
            if (dir.equals(keep)) return FileVisitResult.CONTINUE;
            try (DirectoryStream<Path> s = Files.newDirectoryStream(dir)) {
              if (!s.iterator().hasNext()) {
                Files.delete(dir);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * legacy {@code email/X} 在新布局下对应 {@code email/X/X}。判断 {@code entry} 是否已是新布局：它本身是目录，且目录内存在同名子条目。
   */
  private boolean isAlreadyMigrated(Path entry) {
    if (!Files.isDirectory(entry)) return false;
    Path candidate = entry.resolve(entry.getFileName().toString());
    return Files.exists(candidate);
  }

  /** 粗略的 email 形态判断；basePath 顶层非 email 命名的条目（如 groups.json）会被跳过。 */
  private boolean looksLikeEmail(String name) {
    return name.contains("@");
  }

  private int migrateGroupsFile(File groupsFile) throws IOException {
    if (!groupsFile.exists()) return 0;
    List<Group> groups = readGroups(groupsFile);
    if (groups == null || groups.isEmpty()) return 0;

    HashMap<String, Boolean> existsCache = new HashMap<>();
    boolean groupsChanged = false;
    int updatedScopes = 0;

    for (Group group : groups) {
      List<String> scopes = new ArrayList<>(group.getScopes());
      List<String> newScopes = new ArrayList<>();
      for (String scope : scopes) {
        String trimmed = scope == null ? "" : scope.trim();
        if (trimmed.isEmpty()) continue;

        String[] parts = trimmed.split("/");
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
          String migrated = trimmed + "/" + parts[1].trim();
          Boolean newExists =
              existsCache.computeIfAbsent(
                  migrated, key -> Files.exists(new File(basePath, key).toPath()));
          if (Boolean.TRUE.equals(newExists)) {
            newScopes.add(migrated);
            updatedScopes++;
          } else {
            newScopes.add(trimmed);
          }
        } else {
          newScopes.add(trimmed);
        }
      }

      List<String> deduped = new ArrayList<>(new LinkedHashSet<>(newScopes));
      if (!Objects.equals(deduped, group.getScopes())) {
        group.setScopes(deduped);
        groupsChanged = true;
      }
    }

    if (groupsChanged) writeGroups(groupsFile, groups);
    return updatedScopes;
  }

  private List<Group> readGroups(File file) {
    try {
      return MAPPER.readValue(file, new TypeReference<List<Group>>() {});
    } catch (IOException e) {
      logger.warning("Failed to read groups.json, skip migration: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  private void writeGroups(File file, List<Group> groups) throws IOException {
    MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, groups);
  }

  private Path uniqueTempPath(Path parentDir, String nameHint) throws IOException {
    String safeHint = nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
    for (int i = 0; i < 50; i++) {
      String suffix = i == 0 ? "" : ("_" + i);
      Path candidate =
          parentDir.resolve(
              LEGACY_TEMP_PREFIX + safeHint + "__" + System.currentTimeMillis() + suffix);
      if (!Files.exists(candidate)) return candidate;
    }
    throw new IOException("Unable to allocate temp path under " + parentDir);
  }

  /** 文件级移动：先 atomic move，失败再普通 rename，再不行就「copy + delete」。 桶式存储的 rename 经常只在同一前缀下有效，跨前缀复制是稳的兜底。 */
  private void moveSafely(Path from, Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      return;
    } catch (AtomicMoveNotSupportedException ignored) {
      // fall through
    } catch (IOException ignored) {
      // fall through
    }
    try {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
      return;
    } catch (IOException ignored) {
      // fall through
    }
    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    Files.delete(from);
  }

  /** 暴露出去给监控/管理接口看的迁移摘要。 */
  public static class MigrationReport {
    public int scanned;
    public int migrated;
    public int skipped;
    public int failed;
    public int groupScopes;
  }
}
