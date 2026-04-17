// Exists because bcrypt with cost 12 is the current OWASP recommendation: slow enough for security, fast enough for UX.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {
    private static final int COST = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    @Override public HashedPassword hash(RawPassword raw) { return new HashedPassword(encoder.encode(raw.value())); }
    @Override public boolean matches(RawPassword raw, HashedPassword hashed) { return encoder.matches(raw.value(), hashed.value()); }
}
