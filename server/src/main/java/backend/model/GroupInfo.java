package backend.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupInfo {
  private String id;
  private String name;
  private String ownerEmail;
  private boolean owner;
  private List<String> admins;
  private List<String> members;
  private List<GroupScopeInfo> scopes;
}
