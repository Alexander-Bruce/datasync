package backend.config;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class CorsConfigurationSourceImpl implements CorsConfigurationSource {
  @Override
  public CorsConfiguration getCorsConfiguration(@NonNull HttpServletRequest request) {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
    // Need to change this to the frontend URL
    corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:5173"));
    corsConfiguration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "DELETE", "PUT", "OPTIONS", "PATCH", "DELETE"));
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setExposedHeaders(
        List.of(
            "Authorization", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
    return corsConfiguration;
  }
}
