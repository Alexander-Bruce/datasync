package backend.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public class Group {
  private String id;
  private String name;
  private String ownerEmail;
  private List<String> admins;
  private List<String> members;
  private List<String> scopes;

  public Group() {}

  public Group(
      String id,
      String name,
      String ownerEmail,
      List<String> admins,
      List<String> members,
      List<String> scopes) {
    this.id = id;
    this.name = name;
    this.ownerEmail = ownerEmail;
    this.admins = admins;
    this.members = members;
    this.scopes = scopes;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public List<String> getAdmins() {
    if (admins == null) admins = new ArrayList<>();
    return admins;
  }

  public void setAdmins(List<String> admins) {
    this.admins = admins;
  }

  public List<String> getMembers() {
    if (members == null) members = new ArrayList<>();
    return members;
  }

  public void setMembers(List<String> members) {
    this.members = members;
  }

  public List<String> getScopes() {
    if (scopes == null) scopes = new ArrayList<>();
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }
}
