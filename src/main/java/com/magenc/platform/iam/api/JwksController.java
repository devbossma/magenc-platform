// Exists because any service that needs to verify Magenc JWTs can fetch our public key here without needing the private key.
package com.magenc.platform.iam.api;

import com.magenc.platform.iam.infrastructure.JwtKeyManager;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {
    private final JwtKeyManager keyManager;
    public JwksController(JwtKeyManager keyManager) { this.keyManager = keyManager; }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() { return keyManager.getPublicJwkSet().toJSONObject(); }
}
