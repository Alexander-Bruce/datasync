package backend.migration;

import backend.model.Group;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class ScopeStorageMigrationRunner implements ApplicationRunner {

  private static final Logger logger = Logger.getLogger(ScopeStorageMigrationRunner.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Value("${spring.netty.server.basePath}")
  private String basePath;

  @Override
  public void run(ApplicationArguments args) {
    try {
      migrateLegacyScopesAndStorage();
    } catch (Exception e) {
      logger.warning("Legacy scope migration failed: " + e.getMessage());
    }
  }

  private void migrateLegacyScopesAndStorage() throws IOException {
    File baseDir = new File(basePath);
    if (!baseDir.exists()) {
      baseDir.mkdirs();
    }

    File groupsFile = new File(baseDir, "groups.json");
    if (!groupsFile.exists()) {
      return;
    }

    List<Group> groups = readGroups(groupsFile);
    if (groups == null || groups.isEmpty()) {
      return;
    }

    Map<String, MigrationOutcome> migrationCache = new HashMap<>();
    boolean groupsChanged = false;

    for (Group group : groups) {
      List<String> scopes = new ArrayList<>(group.getScopes());
      List<String> newScopes = new ArrayList<>();
      for (String scope : scopes) {
        String trimmed = scope == null ? "" : scope.trim();
        if (trimmed.isEmpty()) continue;

        String[] parts = trimmed.split("/");
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
          String email = parts[0].trim();
          String rootName = parts[1].trim();
          String oldScope = email + "/" + rootName;
          String migrated = oldScope + "/" + rootName;

          MigrationOutcome outcome =
              migrationCache.computeIfAbsent(
                  oldScope,
                  key -> migrateStoragePath(baseDir.toPath(), key, migrated));

          if (outcome.isSuccess()) {
            newScopes.add(migrated);
            groupsChanged = true;
          } else {
            newScopes.add(oldScope);
          }
        } else {
          newScopes.add(trimmed);
        }
      }

      // de-dup and keep order
      List<String> deduped = new ArrayList<>(new LinkedHashSet<>(newScopes));
      if (!Objects.equals(deduped, group.getScopes())) {
        group.setScopes(deduped);
        groupsChanged = true;
      }
    }

    if (groupsChanged) {
      writeGroups(groupsFile, groups);
      long migratedCount =
          migrationCache.values().stream().filter(MigrationOutcome::isSuccess).count();
      logger.info("Legacy group scopes migrated: " + migratedCount);
    }
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

      // Old layout: base/email/rootName
      // New layout (alias == rootName): base/email/rootName/rootName
      Path aliasDir = emailDir.resolve(rootName);
      Path finalTarget = to;

      // Move the old path out of the way first so we can create the alias directory.
      Path temp = uniqueTempPath(emailDir, rootName);
      tryMove(from, temp);

      Files.createDirectories(aliasDir);
      tryMove(temp, finalTarget);
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
          parentDir.resolve(".__legacy__" + safeHint + "__" + System.currentTimeMillis() + suffix);
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
    FAILED;

    boolean isSuccess() {
      return this == MOVED || this == ALREADY;
    }
  }
}
