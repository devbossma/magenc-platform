package com.magenc.platform.iam.api;

import com.magenc.platform.iam.infrastructure.JwtKeyManager;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the JWT signing key's PUBLIC half as a JWK Set.
 *
 * <p>Standard endpoint location: {@code /.well-known/jwks.json}.
 *
 * <p>This is consumed by:
 * <ul>
 *   <li>This same Spring Boot instance — Spring Security's NimbusJwtDecoder
 *       fetches it on startup to validate incoming tokens (we wire it
 *       directly via the JwtKeyManager today, but the URL works too)</li>
 *   <li>Future microservices that need to validate Magenc-issued JWTs
 *       without sharing the private key</li>
 *   <li>External token introspection tools during debugging</li>
 * </ul>
 *
 * <p>The private key is NEVER exposed here. Only the public modulus and
 * exponent. Anyone can verify a token; only this service can issue one.
 */
@RestController
public class JwksController {

    private final JwtKeyManager keyManager;

    public JwksController(JwtKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return keyManager.getPublicJwkSet().toJSONObject();
    }
}
