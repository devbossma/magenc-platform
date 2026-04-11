package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bcrypt-backed implementation of {@link PasswordHasher}.
 *
 * <p>Cost factor 12 is the current OWASP recommendation as of 2026 — high
 * enough to defeat brute force, fast enough that login UX is acceptable
 * (~250ms on modern hardware). Revisit annually.
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private static final int COST = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    @Override
    public HashedPassword hash(RawPassword raw) {
        return new HashedPassword(encoder.encode(raw.value()));
    }

    @Override
    public boolean matches(RawPassword raw, HashedPassword hashed) {
        return encoder.matches(raw.value(), hashed.value());
    }
}
