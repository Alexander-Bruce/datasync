package backend.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableAsync
public class SecurityConfig implements WebMvcConfigurer {

  @Autowired private UserDetailsService userDetailsService;

  @Autowired @Lazy private AuthenticationEntryPoint authEntryPoint;

  /**
   * 密码加密
   *
   * @return BCryptPasswordEncoder
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * 安全过滤器链
   *
   * @param http HttpSecurity
   * @return 安全过滤器链
   * @throws Exception 异常
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    return http.csrf(customizer -> customizer.disable())
        .authorizeHttpRequests(
            request -> request.requestMatchers("/**").permitAll().anyRequest().authenticated())
        .httpBasic(httpBasic -> httpBasic.disable()) // dev mode, easy for debug by view
        // httpBasic(Customizer.withDefaults()) // prod mode, disable view for security
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // .addFilterBefore(rateLimitingFilter(), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exceptionHandling -> exceptionHandling.authenticationEntryPoint(authEntryPoint))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .build();
  }

  /**
   * 认证提供者
   *
   * @param passwordEncoder 密码加密
   * @return DaoAuthenticationProvider 认证提供者
   */
  @Bean
  public AuthenticationProvider authenticationProvider(BCryptPasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setPasswordEncoder(passwordEncoder);
    provider.setUserDetailsService(userDetailsService);

    return provider;
  }

  /**
   * 认证管理器
   *
   * @return 认证管理器
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 允许的前端地址，显式写出来，不要只用 *
    configuration.setAllowedOrigins(
        Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));

    // 也就是这一行：允许所有来源（备用方案）
    configuration.addAllowedOriginPattern("*");

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

    // 允许所有头
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // 允许 Credentials (Token/Cookie)
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
