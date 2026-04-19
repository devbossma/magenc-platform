// Exists because this wires JWT validation, CORS, public/private endpoint matrix, security headers, and session revocation checking.
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    private final JwtKeyManager jwtKeyManager;

    public SecurityConfig(JwtKeyManager jwtKeyManager) { this.jwtKeyManager = jwtKeyManager; }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)  // safe because auth uses Authorization header, not cookies
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Exists because standard security headers are a 5-line win for defense in depth.
                .headers(headers -> headers
                        .contentTypeOptions(c -> {}) // X-Content-Type-Options: nosniff
                        .frameOptions(f -> f.sameOrigin()) // X-Frame-Options: SAMEORIGIN
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(h -> h
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000L))  // 1 year
                        .permissionsPolicy(p -> p.policy("geolocation=(), microphone=(), camera=()"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v1/health/**",
                                "/v1/auth/signup",
                                "/v1/auth/login",
                                "/v1/auth/discover",
                                "/v1/auth/refresh",
                                "/.well-known/jwks.json",
                                "/actuator/health/**",
                                "/actuator/info",
                                // TODO: disable Swagger in prod profile
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder())
                                .jwtAuthenticationConverter(new MagencJwtAuthenticationConverter()))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(jwtKeyManager.getSigningKey().toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWT decoder", e);
        }
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // TODO: prod profile should restrict to *.orchestrate.marketing only.
        // localhost patterns are required for Next.js dev server.
        config.setAllowedOriginPatterns(List.of(
                "https://*.orchestrate.marketing", "https://orchestrate.marketing",
                "http://*.magenc.local:3000", "http://magenc.local:3000", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type",
                "X-Tenant-Slug", "X-Client-Platform", "X-Client-Version", "X-Request-Id"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
