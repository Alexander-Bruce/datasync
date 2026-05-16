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
public class GroupScopeInfo {
  private String scopeName;
  private String alias;
  private String rootName;
  private String displayName;
  private List<GroupFileNode> files;
}
