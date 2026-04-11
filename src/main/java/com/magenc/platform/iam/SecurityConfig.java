package com.magenc.platform.iam;

import com.magenc.platform.iam.infrastructure.JwtKeyManager;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration. REPLACES the placeholder SecurityConfig
 * from the initial skeleton.
 *
 * <p>Architecture:
 * <ul>
 *   <li>Stateless: no server-side sessions</li>
 *   <li>JWT validation via {@code oauth2-resource-server}</li>
 *   <li>Public: signup, login, refresh, health, swagger, jwks</li>
 *   <li>Everything else requires a valid JWT</li>
 *   <li>CORS for orchestrate.marketing in prod, *.magenc.local in dev</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final JwtKeyManager jwtKeyManager;

    public SecurityConfig(JwtKeyManager jwtKeyManager) {
        this.jwtKeyManager = jwtKeyManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v1/health/**",
                                "/v1/auth/signup",
                                "/v1/auth/login",
                                "/v1/auth/refresh",
                                "/.well-known/jwks.json",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(new MagencJwtAuthenticationConverter())
                        )
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder
                    .withPublicKey(jwtKeyManager.getSigningKey().toRSAPublicKey())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWT decoder", e);
        }
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://*.orchestrate.marketing",
                "https://orchestrate.marketing",
                "http://*.magenc.local:3000",
                "http://magenc.local:3000",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Tenant-Slug",
                "X-Client-Platform", "X-Client-Version", "X-Request-Id"
        ));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
