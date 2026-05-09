package backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.exception.model.BaseException;
import backend.model.Group;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class GroupServiceImplTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @TempDir Path tempDir;

  @Test
  void scopeLinkedToAnyGroupIsNotDeletableEvenWithoutMembers() throws Exception {
    GroupServiceImpl service = serviceWithTempStorage();
    writeGroups(List.of(group("g1", List.of(), List.of("owner@example.com/Documents"))));

    assertFalse(service.isScopeDeletable("owner@example.com/Documents"));
  }

  @Test
  void scopeNotLinkedToGroupIsDeletable() throws Exception {
    GroupServiceImpl service = serviceWithTempStorage();
    writeGroups(
        List.of(
            group("g1", List.of("member@example.com"), List.of("owner@example.com/Documents"))));

    assertTrue(service.isScopeDeletable("owner@example.com/Pictures"));
  }

  @Test
  void adminCanSeeGroupFiles() throws Exception {
    GroupServiceImpl service = serviceWithTempStorage();
    writeGroups(
        List.of(
            group(
                "g1",
                List.of("admin@example.com"),
                List.of("member@example.com"),
                List.of("owner@example.com/Documents"))));

    List<backend.model.GroupInfo> groups = service.getGroupFiles("admin@example.com");

    assertEquals(1, groups.size());
    assertEquals("g1", groups.get(0).getId());
    assertEquals(List.of("admin@example.com"), groups.get(0).getAdmins());
  }

  @Test
  void ownerCannotBeChangedThroughRemoveAdmin() throws Exception {
    GroupServiceImpl service = serviceWithTempStorage();
    writeGroups(List.of(group("g1", List.of(), List.of())));

    BaseException ex =
        assertThrows(
            BaseException.class,
            () -> service.removeAdmin("owner@example.com", "g1", " owner@example.com "));

    assertEquals(409, ex.getCode());
  }

  @Test
  void removeAdminDoesNotAddNonAdminAsMember() throws Exception {
    GroupServiceImpl service = serviceWithTempStorage();
    writeGroups(List.of(group("g1", List.of(), List.of())));

    Group updated = service.removeAdmin("owner@example.com", "g1", "visitor@example.com");

    assertTrue(updated.getMembers().isEmpty());
  }

  private GroupServiceImpl serviceWithTempStorage() {
    GroupServiceImpl service = new GroupServiceImpl();
    ReflectionTestUtils.setField(service, "basePath", tempDir.toString());
    return service;
  }

  private void writeGroups(List<Group> groups) throws Exception {
    MAPPER.writeValue(new File(tempDir.toFile(), "groups.json"), groups);
  }

  private Group group(String id, List<String> members, List<String> scopes) {
    return group(id, List.of(), members, scopes);
  }

  private Group group(String id, List<String> admins, List<String> members, List<String> scopes) {
    return new Group(
        id,
        "Team",
        "owner@example.com",
        new ArrayList<>(admins),
        new ArrayList<>(members),
        new ArrayList<>(scopes));
  }
}
