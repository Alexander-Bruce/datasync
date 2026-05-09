package backend.interceptor;

import backend.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${spring.jwt.secretkey}")
  private String secretkey;

  @Value("${spring.jwt.accessTokenExpiration}")
  private long accessTokenExpiration;

  @Value("${spring.jwt.freshTokenExpiration}")
  private long freshTokenExpiration;

  @Value("${spring.jwt.uid}")
  private String uid;

  @Value("role")
  private String role;

  /**
   * 生成token
   *
   * @param username 用户名
   * @param type token类型 1: access token 2: fresh token
   * @return token
   */
  public String generateToken(String username, Integer type) {
    long expiration = type == 1 ? accessTokenExpiration : freshTokenExpiration;
    Map<String, Object> claims = new HashMap<>();
    return Jwts.builder()
        .claims()
        .add(claims)
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .and()
        .signWith(getKey())
        .compact();
  }

  public String generateToken(Integer userId, Integer userRole, Integer type) {
    long expiration = type == 1 ? accessTokenExpiration : freshTokenExpiration;

    // Saved for future usage
    Map<String, Object> claims = new HashMap<>();
    claims.put(uid, userId);
    claims.put(role, userRole);

    return Jwts.builder()
        .claims()
        .add(claims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .and()
        .signWith(getKey())
        .compact();
  }

  public String generateToken(Integer userId) {
    long expiration = freshTokenExpiration;

    // Saved for future usage
    Map<String, Object> claims = new HashMap<>();
    claims.put(uid, userId);

    return Jwts.builder()
        .claims()
        .add(claims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .and()
        .signWith(getKey())
        .compact();
  }

  /**
   * 获取密钥
   *
   * @return 密钥
   */
  private SecretKey getKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretkey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * 从token中提取用户名
   *
   * @param token token
   * @return 用户名
   */
  public String extractUserName(String token) {
    // extract the username from jwt token
    try {
      return extractClaim(token, Claims::getSubject);
    } catch (Exception e) {
      throw new RuntimeException("Invalid JWT token");
    }
  }

  public Integer extractUid(String token) {
    return (Integer) extractPrivateClaim(token, uid);
  }

  public Integer extractRole(String token) {
    return (Integer) extractPrivateClaim(token, role);
  }

  private Object extractPrivateClaim(String token, String fieldName) {
    final Claims claims = extractAllClaims(token);
    return claims.get(fieldName);
  }

  /**
   * 从token中提取过期时间
   *
   * @param token token
   * @return 过期时间
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
  }

  /**
   * 从token中提取所有声明
   *
   * @param token token
   * @return 所有声明
   */
  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    } catch (Exception e) {
      throw new RuntimeException("Invalid JWT token");
    }
  }

  /**
   * 验证token
   *
   * @param token token
   * @param userDetails 用户信息
   * @return 是否有效
   */
  public boolean validateToken(String token, UserDetails userDetails) {
    final String userName = extractUserName(token);
    return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public String extractId(String token) {
    return extractUserName(token);
  }

  public boolean validateTokenById(String token, User user) {
    Integer tokenUid = extractUid(token);
    Integer userUid = user.getId();
    if (tokenUid == null || userUid == null) return false;
    return tokenUid.equals(userUid) && !isTokenExpired(token);
  }

  /**
   * 判断token是否过期
   *
   * @param token token
   * @return 是否过期
   */
  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * 从token中提取过期时间
   *
   * @param token token
   * @return 过期时间
   */
  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }
}
