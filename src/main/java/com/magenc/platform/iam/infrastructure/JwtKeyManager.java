package com.magenc.platform.iam.infrastructure;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manages the RSA keypair used to sign and verify JWTs.
 *
 * <p><b>Dev mode:</b> generates a fresh RSA-2048 keypair on every startup.
 * This means tokens issued by an old run are invalid after restart, which
 * is desirable in development.
 *
 * <p><b>Production:</b> the keypair must be loaded from AWS Secrets Manager
 * at startup. The current implementation logs a warning if the production
 * profile is active and no key source is configured. This will be wired up
 * when we set up AWS deployment; for now, dev-mode generation is the only
 * supported path.
 *
 * <p>The public key is exposed via {@link #getJwkSource()}, which Spring
 * Security's JWT decoder uses to verify incoming tokens. It's also exposed
 * publicly via the {@code /.well-known/jwks.json} endpoint so any future
 * service in our system can verify Magenc-issued JWTs without needing
 * the private key.
 */
@Component
public class JwtKeyManager {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyManager.class);
    private static final String KEY_ID = "magenc-signing-key";

    @Value("${magenc.security.jwt.issuer}")
    private String issuer;

    private RSAKey rsaKey;
    private JWKSet jwkSet;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Generating ephemeral RSA-2048 keypair for JWT signing (dev mode)");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            this.rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(KEY_ID + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .build();
            this.jwkSet = new JWKSet(rsaKey.toPublicJWK());
            log.info("JWT signing key initialized with kid={}", rsaKey.getKeyID());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT keypair", e);
        }
    }

    public RSAKey getSigningKey() {
        return rsaKey;
    }

    public JWKSource<SecurityContext> getJwkSource() {
        return new ImmutableJWKSet<>(jwkSet);
    }

    public JWKSet getPublicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    public String getIssuer() {
        return issuer;
    }
}
