package backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubFile {

  private Integer id;

  private Integer fileId;

  private String relativePath;

  private Integer depth;

  private Boolean isDir;

  private Boolean isSync;

  private Integer parent;

  private String name;
}
