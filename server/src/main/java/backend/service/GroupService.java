package backend.service;

import backend.model.Group;
import backend.model.GroupInfo;
import java.util.List;

public interface GroupService {
  Group createGroup(String email, String name);

  Group addMember(String email, String groupId, String memberEmail);

  Group addMembers(String email, String groupId, List<String> memberEmails);

  Group removeMember(String email, String groupId, String memberEmail);

  Group removeMembers(String email, String groupId, List<String> memberEmails);

  Group addAdmin(String ownerEmail, String groupId, String adminEmail);

  Group removeAdmin(String ownerEmail, String groupId, String adminEmail);

  Group addScope(String email, String groupId, String scopeName);

  Group removeScope(String email, String groupId, String scopeName);

  boolean deleteGroup(String email, String groupId);

  List<Group> listGroups(String email);

  List<GroupInfo> getGroupFiles(String email);

  boolean isScopeDeletable(String scopeName);
}
