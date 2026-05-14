package backend.migration;

import backend.model.Group;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 把旧 {@code email/rootName} 布局迁移到新的 {@code email/alias/rootName} 布局，迁移在容器启动时执行一次，幂等安全。
 *
 * <p>对于没有引入「显式 alias」的历史数据，alias 就是 rootName 自身（旧客户端的默认行为），所以 {@code email/X → email/X/X}
 * 是无损可逆的等价变换。私有任务（不在 groups.json 中）也会被遍历，这样桶里所有遗留 scope 都能跟上新代码而不产生孤儿文件。
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
      migrateAll();
    } catch (Exception e) {
      logger.warning("Legacy scope migration failed: " + e.getMessage());
    }
  }

  private void migrateAll() throws IOException {
    File baseDir = new File(basePath);
    if (!baseDir.exists() && !baseDir.mkdirs()) {
      logger.warning("Migration skipped: cannot create basePath " + basePath);
      return;
    }

    // 1. 遍历存储桶里所有 email 目录，迁移本地（私有 + 群组）所有遗留 scope。
    int storageMigrated = migrateAllEmailDirectories(baseDir.toPath());

    // 2. 再把 groups.json 中已记录的 scope 名称做对应更新，让群组共享继续指向新路径。
    int groupsMigrated = migrateGroupsFile(new File(baseDir, "groups.json"));

    if (storageMigrated > 0 || groupsMigrated > 0) {
      logger.info(
          "Legacy scope migration complete: storageEntries="
              + storageMigrated
              + ", groupScopes="
              + groupsMigrated);
    }
  }

  /**
   * 扫描 {@code basePath/<email>/*}，对每个尚未迁移的 scope 执行 {@code email/X → email/X/X} 的转换。已迁移项（{@code
   * email/X/X} 存在）会被跳过，保证再次启动也不会破坏数据。
   */
  private int migrateAllEmailDirectories(Path baseDir) throws IOException {
    int migrated = 0;
    try (DirectoryStream<Path> emails = Files.newDirectoryStream(baseDir)) {
      for (Path emailDir : emails) {
        String emailName = emailDir.getFileName().toString();
        if (!Files.isDirectory(emailDir)) continue;
        if (emailName.startsWith(LEGACY_TEMP_PREFIX)) continue;
        if (!looksLikeEmail(emailName)) continue;

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(emailDir)) {
          List<Path> children = new ArrayList<>();
          entries.forEach(children::add);
          // 按名字稳定排序，便于日志可重复
          children.sort(Comparator.comparing(p -> p.getFileName().toString()));

          for (Path entry : children) {
            String entryName = entry.getFileName().toString();
            if (entryName.startsWith(LEGACY_TEMP_PREFIX)) continue;
            if (isAlreadyMigrated(entry)) continue;

            String oldScope = emailName + "/" + entryName;
            String newScope = oldScope + "/" + entryName;
            MigrationOutcome outcome = migrateStoragePath(baseDir, oldScope, newScope);
            if (outcome == MigrationOutcome.MOVED) {
              migrated++;
            }
          }
        }
      }
    }
    return migrated;
  }

  /**
   * 一个旧 scope 路径 {@code email/X} 在新布局下对应 {@code email/X/X}。判断 {@code entry} 是否已是新布局的 alias
   * 目录：它本身必须是目录，且目录内存在与目录同名的子条目（即 rootName == alias 的 {@code email/X/X}）。这是历史数据迁移过来后的稳态。
   */
  private boolean isAlreadyMigrated(Path entry) {
    if (!Files.isDirectory(entry)) return false;
    Path candidate = entry.resolve(entry.getFileName().toString());
    return Files.exists(candidate);
  }

  /**
   * 过滤掉 {@code groups.json} 这种非 email 顶层条目。判断标准很宽松：包含 {@code @} 即可。 历史上 email 一定是顶层目录命名，所以这个粗判别足够。
   */
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
          // 仅当迁移后的目录确实在磁盘上存在时才改写 scope 名，否则保留原 scope，避免群组共享指向不存在的路径。
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

    if (groupsChanged) {
      writeGroups(groupsFile, groups);
    }
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

  private MigrationOutcome migrateStoragePath(Path baseDir, String oldScope, String newScope) {
    Path from = baseDir.resolve(oldScope);
    Path to = baseDir.resolve(newScope);

    if (Files.exists(to)) {
      return MigrationOutcome.ALREADY;
    }
    if (!Files.exists(from)) {
      return MigrationOutcome.MISSING;
    }

    try {
      Path emailDir = from.getParent();
      String rootName = from.getFileName().toString();
      if (emailDir == null || rootName.isBlank()) {
        return MigrationOutcome.FAILED;
      }

      // 旧布局可能是文件（单文件任务）或目录（文件夹任务），都用「先挪开 -> 建 alias 目录 -> 挪回去」三步完成。
      Path temp = uniqueTempPath(emailDir, rootName);
      tryMove(from, temp);

      Files.createDirectories(to.getParent());
      tryMove(temp, to);

      logger.info("Migrated storage path: " + oldScope + " -> " + newScope);
      return MigrationOutcome.MOVED;
    } catch (IOException e) {
      logger.warning("Failed migrating storage path " + oldScope + " -> " + newScope + ": " + e);
      return MigrationOutcome.FAILED;
    }
  }

  private Path uniqueTempPath(Path parentDir, String nameHint) throws IOException {
    String safeHint = nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
    for (int i = 0; i < 50; i++) {
      String suffix = i == 0 ? "" : ("_" + i);
      Path candidate =
          parentDir.resolve(
              LEGACY_TEMP_PREFIX + safeHint + "__" + System.currentTimeMillis() + suffix);
      if (!Files.exists(candidate)) {
        return candidate;
      }
    }
    throw new IOException("Unable to allocate temp path under " + parentDir);
  }

  private void tryMove(Path from, Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to);
    }
  }

  private enum MigrationOutcome {
    MOVED,
    ALREADY,
    MISSING,
    FAILED
  }
}
