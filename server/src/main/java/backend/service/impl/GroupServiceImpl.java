package backend.service.impl;

import backend.exception.model.BaseException;
import backend.mapper.mysql.UserMapper;
import backend.model.*;
import backend.service.GroupService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ReentrantLock groupFileLock = new ReentrantLock();

  @Value("${spring.netty.server.basePath}")
  private String basePath;

  @Autowired private UserMapper userMapper;

  private File groupsFile() {
    File dir = new File(basePath);
    dir.mkdirs();
    return new File(dir, "groups.json");
  }

  private List<Group> readGroups() {
    File file = groupsFile();
    if (!file.exists()) return new ArrayList<>();
    try {
      return MAPPER.readValue(file, new TypeReference<List<Group>>() {});
    } catch (IOException e) {
      return new ArrayList<>();
    }
  }

  private void writeGroups(List<Group> groups) {
    File file = groupsFile();
    try {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, groups);
    } catch (IOException e) {
      throw new BaseException("Failed to write groups file: " + e.getMessage(), 500);
    }
  }

  private boolean isOwner(Group g, String email) {
    return g.getOwnerEmail().equals(email);
  }

  private boolean canManage(Group g, String email) {
    return isOwner(g, email) || g.getAdmins().contains(email);
  }

  private Group findGroup(List<Group> groups, String groupId) {
    return groups.stream()
        .filter(g -> g.getId().equals(groupId))
        .findFirst()
        .orElseThrow(() -> new BaseException("Group not found", 404));
  }

  private Group findManageable(List<Group> groups, String email, String groupId) {
    Group g = findGroup(groups, groupId);
    if (!canManage(g, email))
      throw new BaseException("Permission denied: admin or owner required", 403);
    return g;
  }

  private Group findOwned(List<Group> groups, String email, String groupId) {
    Group g = findGroup(groups, groupId);
    if (!isOwner(g, email)) throw new BaseException("Permission denied: owner required", 403);
    return g;
  }

  private String normalizeExistingUserEmail(String email) {
    String trimmed = email == null ? "" : email.trim();
    if (trimmed.isEmpty()) {
      throw new BaseException("Member email is required", 400);
    }
    if (userMapper.selectByEmail(trimmed) == null) {
      throw new BaseException("User does not exist: " + trimmed, 404);
    }
    return trimmed;
  }

  @Override
  public Group createGroup(String email, String name) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group =
          Group.builder()
              .id(UUID.randomUUID().toString())
              .name(name)
              .ownerEmail(email)
              .admins(new ArrayList<>())
              .members(new ArrayList<>())
              .scopes(new ArrayList<>())
              .build();
      groups.add(group);
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group addMember(String email, String groupId, String memberEmail) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      String normalizedEmail = normalizeExistingUserEmail(memberEmail);
      if (!group.getMembers().contains(normalizedEmail)
          && !group.getAdmins().contains(normalizedEmail)
          && !group.getOwnerEmail().equals(normalizedEmail)) {
        group.getMembers().add(normalizedEmail);
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group addMembers(String email, String groupId, List<String> memberEmails) {
    if (memberEmails == null || memberEmails.isEmpty()) {
      throw new BaseException("Member emails are required", 400);
    }
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      List<String> normalizedEmails =
          memberEmails.stream().map(this::normalizeExistingUserEmail).collect(Collectors.toList());
      for (String trimmed : normalizedEmails) {
        if (!group.getMembers().contains(trimmed)
            && !group.getAdmins().contains(trimmed)
            && !group.getOwnerEmail().equals(trimmed)) {
          group.getMembers().add(trimmed);
        }
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group removeMember(String email, String groupId, String memberEmail) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      if (group.getAdmins().contains(memberEmail) && !isOwner(group, email)) {
        throw new BaseException("Permission denied: only owner can remove admins", 403);
      }
      group.getMembers().remove(memberEmail);
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group removeMembers(String email, String groupId, List<String> memberEmails) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      for (String m : memberEmails) {
        String trimmed = m.trim();
        if (group.getAdmins().contains(trimmed) && !isOwner(group, email)) continue;
        group.getMembers().remove(trimmed);
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group addAdmin(String ownerEmail, String groupId, String adminEmail) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findOwned(groups, ownerEmail, groupId);
      String normalizedEmail = normalizeExistingUserEmail(adminEmail);
      if (!group.getAdmins().contains(normalizedEmail)
          && !group.getOwnerEmail().equals(normalizedEmail)) {
        group.getAdmins().add(normalizedEmail);
        group.getMembers().remove(normalizedEmail);
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group removeAdmin(String ownerEmail, String groupId, String adminEmail) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findOwned(groups, ownerEmail, groupId);
      String targetEmail = adminEmail == null ? "" : adminEmail.trim();
      if (targetEmail.isEmpty()) {
        throw new BaseException("Admin email is required", 400);
      }
      if (isOwner(group, targetEmail)) {
        throw new BaseException("Owner cannot be changed", 409);
      }
      boolean removed = group.getAdmins().remove(targetEmail);
      if (removed && !group.getMembers().contains(targetEmail)) {
        group.getMembers().add(targetEmail);
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group addScope(String email, String groupId, String scopeName) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      if (!group.getScopes().contains(scopeName)) {
        group.getScopes().add(scopeName);
      }
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public Group removeScope(String email, String groupId, String scopeName) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findManageable(groups, email, groupId);
      group.getScopes().remove(scopeName);
      writeGroups(groups);
      return group;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public boolean deleteGroup(String email, String groupId) {
    groupFileLock.lock();
    try {
      List<Group> groups = readGroups();
      Group group = findOwned(groups, email, groupId);
      boolean removed = groups.remove(group);
      if (!removed) throw new BaseException("Group not found", 404);
      writeGroups(groups);
      return true;
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public List<Group> listGroups(String email) {
    groupFileLock.lock();
    try {
      return readGroups().stream()
          .filter(
              g ->
                  g.getOwnerEmail().equals(email)
                      || g.getAdmins().contains(email)
                      || g.getMembers().contains(email))
          .collect(Collectors.toList());
    } finally {
      groupFileLock.unlock();
    }
  }

  @Override
  public List<GroupInfo> getGroupFiles(String email) {
    groupFileLock.lock();
    List<Group> groups;
    try {
      groups = readGroups();
    } finally {
      groupFileLock.unlock();
    }
    return groups.stream()
        .filter(
            g ->
                g.getOwnerEmail().equals(email)
                    || g.getAdmins().contains(email)
                    || g.getMembers().contains(email))
        .map(
            g -> {
              List<GroupScopeInfo> scopeInfos =
                  g.getScopes().stream()
                      .map(
                          scopeName ->
                              GroupScopeInfo.builder()
                                  .scopeName(scopeName)
                                  .files(listScopeFiles(scopeName))
                                  .build())
                      .collect(Collectors.toList());
              return GroupInfo.builder()
                  .id(g.getId())
                  .name(g.getName())
                  .ownerEmail(g.getOwnerEmail())
                  .owner(g.getOwnerEmail().equals(email))
                  .admins(g.getAdmins())
                  .members(g.getMembers())
                  .scopes(scopeInfos)
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Override
  public boolean isScopeDeletable(String scopeName) {
    groupFileLock.lock();
    try {
      return readGroups().stream().noneMatch(g -> g.getScopes().contains(scopeName));
    } finally {
      groupFileLock.unlock();
    }
  }

  private List<GroupFileNode> listScopeFiles(String scopeName) {
    File scopeDir = new File(basePath, scopeName);
    if (!scopeDir.exists() || !scopeDir.isDirectory()) return new ArrayList<>();
    List<GroupFileNode> result = new ArrayList<>();
    collectFiles(scopeDir, scopeDir, result);
    return result;
  }

  private void collectFiles(File root, File current, List<GroupFileNode> result) {
    File[] children = current.listFiles();
    if (children == null) return;
    Arrays.sort(children, Comparator.comparing(File::getName));
    for (File child : children) {
      if (child.getName().endsWith(".part")) continue;
      String rel = buildRelativePath(root, child);
      result.add(
          GroupFileNode.builder()
              .name(child.getName())
              .relativePath(rel)
              .dir(child.isDirectory())
              .build());
      if (child.isDirectory()) {
        collectFiles(root, child, result);
      }
    }
  }

  private String buildRelativePath(File root, File file) {
    String rootPath = root.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    if (filePath.startsWith(rootPath)) {
      return filePath.substring(rootPath.length()).replaceAll("^[/\\\\]+", "").replace("\\", "/");
    }
    return file.getName();
  }
}
