package backend.controller;

import backend.service.GroupService;
import backend.util.ResultEntity;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/server/group")
public class GroupController {

  @Autowired private GroupService groupService;

  @PostMapping("/create")
  public ResponseEntity<ResultEntity<Object>> createGroup(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200, "Group created", groupService.createGroup(map.get("email"), map.get("name")));
  }

  @PostMapping("/add-member")
  public ResponseEntity<ResultEntity<Object>> addMember(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Member added",
        groupService.addMember(map.get("email"), map.get("groupId"), map.get("memberEmail")));
  }

  @PostMapping("/remove-member")
  public ResponseEntity<ResultEntity<Object>> removeMember(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Member removed",
        groupService.removeMember(map.get("email"), map.get("groupId"), map.get("memberEmail")));
  }

  @PostMapping("/add-scope")
  public ResponseEntity<ResultEntity<Object>> addScope(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Scope added",
        groupService.addScope(map.get("email"), map.get("groupId"), map.get("scopeName")));
  }

  @PostMapping("/remove-scope")
  public ResponseEntity<ResultEntity<Object>> removeScope(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Scope removed",
        groupService.removeScope(map.get("email"), map.get("groupId"), map.get("scopeName")));
  }

  @PostMapping("/delete")
  public ResponseEntity<ResultEntity<Object>> deleteGroup(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200, "Group deleted", groupService.deleteGroup(map.get("email"), map.get("groupId")));
  }

  @PostMapping("/list")
  public ResponseEntity<ResultEntity<Object>> listGroups(@RequestBody Map<String, String> map) {
    return ResultEntity.success(200, "Groups fetched", groupService.listGroups(map.get("email")));
  }

  @PostMapping("/add-admin")
  public ResponseEntity<ResultEntity<Object>> addAdmin(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Admin added",
        groupService.addAdmin(map.get("email"), map.get("groupId"), map.get("adminEmail")));
  }

  @PostMapping("/remove-admin")
  public ResponseEntity<ResultEntity<Object>> removeAdmin(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200,
        "Admin removed",
        groupService.removeAdmin(map.get("email"), map.get("groupId"), map.get("adminEmail")));
  }

  @PostMapping("/add-members")
  public ResponseEntity<ResultEntity<Object>> addMembers(@RequestBody Map<String, Object> map) {
    @SuppressWarnings("unchecked")
    List<String> emails = (List<String>) map.get("memberEmails");
    return ResultEntity.success(
        200,
        "Members added",
        groupService.addMembers((String) map.get("email"), (String) map.get("groupId"), emails));
  }

  @PostMapping("/remove-members")
  public ResponseEntity<ResultEntity<Object>> removeMembers(@RequestBody Map<String, Object> map) {
    @SuppressWarnings("unchecked")
    List<String> emails = (List<String>) map.get("memberEmails");
    return ResultEntity.success(
        200,
        "Members removed",
        groupService.removeMembers((String) map.get("email"), (String) map.get("groupId"), emails));
  }

  @PostMapping("/files")
  public ResponseEntity<ResultEntity<Object>> getGroupFiles(@RequestBody Map<String, String> map) {
    return ResultEntity.success(
        200, "Group files fetched", groupService.getGroupFiles(map.get("email")));
  }

  @PostMapping("/check-scope")
  public ResponseEntity<ResultEntity<Object>> checkScope(@RequestBody Map<String, String> map) {
    boolean deletable = groupService.isScopeDeletable(map.get("scopeName"));
    return ResultEntity.success(200, "Checked", deletable);
  }
}
