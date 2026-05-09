package backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

  private Integer id;

  private String username;

  private String email;

  private String avatar;

  private String userAgent;

  private String refreshToken;

  private String accessToken;
}
