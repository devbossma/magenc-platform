// Exists because JWT signing keys must be managed centrally: dev generates ephemeral keys, prod loads from AWS Secrets Manager.
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

@Component
public class JwtKeyManager {
    private static final Logger log = LoggerFactory.getLogger(JwtKeyManager.class);

    @Value("${magenc.security.jwt.issuer}")
    private String issuer;

    private RSAKey rsaKey;
    private JWKSet jwkSet;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Generating ephemeral RSA-2048 keypair for JWT signing (dev mode)");
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            this.rsaKey = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID("magenc-signing-key-" + UUID.randomUUID().toString().substring(0, 8))
                    .build();
            this.jwkSet = new JWKSet(rsaKey.toPublicJWK());
            log.info("JWT signing key initialized with kid={}", rsaKey.getKeyID());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT keypair", e);
        }
    }

    public RSAKey getSigningKey() { return rsaKey; }
    public JWKSource<SecurityContext> getJwkSource() { return new ImmutableJWKSet<>(jwkSet); }
    public JWKSet getPublicJwkSet() { return new JWKSet(rsaKey.toPublicJWK()); }
    public String getIssuer() { return issuer; }
}
