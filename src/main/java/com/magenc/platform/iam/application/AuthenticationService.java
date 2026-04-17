// Exists because login is the most security-sensitive operation: this service enforces constant-time rejection and logs every attempt.
package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.AuthenticationFailedException;
import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.PlatformUser;
import com.magenc.platform.iam.domain.PlatformUserRepository;
import com.magenc.platform.iam.domain.RawPassword;
import com.magenc.platform.iam.domain.Session;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final PlatformUserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AuthEventLogger authEventLogger;
    private final JdbcTemplate jdbcTemplate;

    public AuthenticationService(
            PlatformUserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            SessionService sessionService,
            AuthEventLogger authEventLogger,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.authEventLogger = authEventLogger;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Scoped login: user knows which agency they want to access.
     * Validates credentials, checks membership, issues scoped JWT.
     */
    @Transactional
    public TokenService.TokenPair authenticateScoped(
            String tenantSlug, Email email, RawPassword password,
            String userAgent, String ipAddress) {

        PlatformUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    authEventLogger.logFailure("LOGIN_SCOPED", null, null,
                            "USER_NOT_FOUND", ipAddress, userAgent);
                    return new AuthenticationFailedException();
                });

        if (!passwordHasher.matches(password, user.password())) {
            authEventLogger.logFailure("LOGIN_SCOPED", user.id().value(), null,
                    "BAD_PASSWORD", ipAddress, userAgent);
            throw new AuthenticationFailedException();
        }

        if (!user.canSignIn()) {
            authEventLogger.logFailure("LOGIN_SCOPED", user.id().value(), null,
                    "ACCOUNT_DISABLED", ipAddress, userAgent);
            throw new AuthenticationFailedException();
        }

        MembershipInfo membership = findActiveMembership(user.id().value(), tenantSlug);
        if (membership == null) {
            authEventLogger.logFailure("LOGIN_SCOPED", user.id().value(), null,
                    "NO_MEMBERSHIP", ipAddress, userAgent);
            throw new AuthenticationFailedException();
        }

        user.recordLogin(Instant.now());
        userRepository.save(user);

        Session session = sessionService.createSession(
                user.id(), membership.agencyId(), membership.membershipId(),
                userAgent, ipAddress);

        TokenService.TokenPair tokens = tokenService.issueTokens(
                user, tenantSlug, membership.agencyId(),
                membership.membershipId(), membership.role(), session.id());

        authEventLogger.logSuccess("LOGIN_SCOPED", user.id().value(),
                membership.agencyId(), session.id().value(), ipAddress, userAgent);

        return tokens;
    }

    /**
     * Discovery login: user doesn't specify an agency. Validates credentials
     * and returns the list of agencies the user can access. The frontend
     * then calls /v1/auth/scope to pick one and get a full JWT.
     */
    @Transactional
    public DiscoveryResult authenticateDiscovery(Email email, RawPassword password,
                                                  String userAgent, String ipAddress) {

        PlatformUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    authEventLogger.logFailure("LOGIN_DISCOVERY", null, null,
                            "USER_NOT_FOUND", ipAddress, userAgent);
                    return new AuthenticationFailedException();
                });

        if (!passwordHasher.matches(password, user.password())) {
            authEventLogger.logFailure("LOGIN_DISCOVERY", user.id().value(), null,
                    "BAD_PASSWORD", ipAddress, userAgent);
            throw new AuthenticationFailedException();
        }

        if (!user.canSignIn()) {
            authEventLogger.logFailure("LOGIN_DISCOVERY", user.id().value(), null,
                    "ACCOUNT_DISABLED", ipAddress, userAgent);
            throw new AuthenticationFailedException();
        }

        List<MembershipInfo> memberships = findAllActiveMemberships(user.id().value());

        user.recordLogin(Instant.now());
        userRepository.save(user);

        authEventLogger.logSuccess("LOGIN_DISCOVERY", user.id().value(),
                null, null, ipAddress, userAgent);

        return new DiscoveryResult(user, memberships);
    }

    private MembershipInfo findActiveMembership(UUID userId, String tenantSlug) {
        var rows = jdbcTemplate.query("""
                SELECT am.id, am.agency_id, am.role, a.slug, a.display_name
                FROM admin.agency_membership am
                JOIN admin.agency a ON a.id = am.agency_id
                WHERE am.platform_user_id = ? AND a.slug = ? AND am.status = 'ACTIVE' AND a.status = 'ACTIVE'
                """,
                (rs, rowNum) -> new MembershipInfo(
                        rs.getObject("id", UUID.class),
                        rs.getObject("agency_id", UUID.class),
                        rs.getString("role"),
                        rs.getString("slug"),
                        rs.getString("display_name")),
                userId, tenantSlug);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<MembershipInfo> findAllActiveMemberships(UUID userId) {
        return jdbcTemplate.query("""
                SELECT am.id, am.agency_id, am.role, a.slug, a.display_name
                FROM admin.agency_membership am
                JOIN admin.agency a ON a.id = am.agency_id
                WHERE am.platform_user_id = ? AND am.status = 'ACTIVE' AND a.status = 'ACTIVE'
                ORDER BY am.joined_at DESC
                """,
                (rs, rowNum) -> new MembershipInfo(
                        rs.getObject("id", UUID.class),
                        rs.getObject("agency_id", UUID.class),
                        rs.getString("role"),
                        rs.getString("slug"),
                        rs.getString("display_name")),
                userId);
    }

    public record MembershipInfo(UUID membershipId, UUID agencyId, String role,
                                  String slug, String displayName) {}

    public record DiscoveryResult(PlatformUser user, List<MembershipInfo> memberships) {}
}
