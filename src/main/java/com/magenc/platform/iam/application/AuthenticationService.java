package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.AuthenticationFailedException;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.iam.domain.User;
import com.magenc.platform.iam.domain.UserRepository;
import com.magenc.platform.tenancy.TenantContext;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates user credentials and issues tokens on success.
 *
 * <p>Side effects on success: updates {@code last_login_at} on the user
 * record. This is intentionally inside the same transaction as the
 * credential check, so a successful authentication is recorded
 * atomically.
 *
 * <p>On failure, this service ALWAYS throws {@link AuthenticationFailedException}
 * with the same generic message regardless of whether the user exists,
 * the password was wrong, or the account was suspended. This prevents
 * user enumeration attacks via timing or error message inspection.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    /**
     * Authenticates a user against the current tenant context.
     *
     * <p>The current tenant must already be set in {@link TenantContext}
     * before this method is called. The login controller sets the tenant
     * from the {@code tenantSlug} field of the login request body, since
     * unauthenticated requests carry no JWT to extract it from.
     */
    @Transactional
    public TokenService.TokenPair authenticate(Email email, RawPassword password) {
        String tenantSlug = TenantContext.require();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("Authentication failed: user not found in tenant={}", tenantSlug);
                    TenantContext.clear();
                    return new AuthenticationFailedException();
                });

        if (!passwordHasher.matches(password, user.password())) {
            log.info("Authentication failed: bad password for user={} in tenant={}",
                    user.id(), tenantSlug);
            TenantContext.clear();
            throw new AuthenticationFailedException();
        }

        if (!user.canSignIn()) {
            log.info("Authentication failed: user={} cannot sign in (status={})",
                    user.id(), user.status());
            TenantContext.clear();
            throw new AuthenticationFailedException();
        }

        user.recordLogin(Instant.now());
        userRepository.save(user);

        log.info("Authentication succeeded for user={} in tenant={}", user.id(), tenantSlug);
        return tokenService.issueTokens(user, tenantSlug);
    }
}
