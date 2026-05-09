package backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class File {
  private Integer id;

  private String alias;

  private String path;

  private String remoteHost;

  private String scheduled;

  private String cdcAlg;

  private Boolean isDir;

  private Boolean isSync;

  private String description;

  private String updateTime;

  private Integer userId;
}
